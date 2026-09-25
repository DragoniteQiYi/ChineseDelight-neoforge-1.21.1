/*
 * 中华乐事 · 数据校验器
 * 用法：node tools/cooking/validate.js
 *
 * 检查：
 *   1. 区间是否合法（min <= max）
 *   2. 引用的模组物品是否登记过
 *   3. 是否缺兜底菜 / 缺惩罚料理
 *   4. 采样模拟：找出被遮蔽的菜，并统计「出失败料理」的比例
 */
const fs = require('fs');
const path = require('path');
const data = require('./data');
const NS = data.NS;

const problems = [];
const warnings = [];

/* ---------------------------------------------------------------- 1. 区间 */

function checkRange(where, axis, range) {
    if (typeof range === 'number') {
        if (range < 0) problems.push(`${where}: ${axis} 的数值是负数 (${range})`);
        return;
    }
    const min = range.min ?? 0;
    const max = range.max ?? Infinity;
    if (min > max) problems.push(`${where}: ${axis} 区间非法 min=${min} > max=${max}`);
    if (min < 0) problems.push(`${where}: ${axis} 的 min 是负数 (${min})`);
}

for (const dish of data.DISHES) {
    for (const [axis, range] of Object.entries(dish.flavors || {})) {
        if (!data.FLAVORS.includes(axis)) problems.push(`菜品 ${dish.id}: 未知味觉轴 ${axis}`);
        checkRange(`菜品 ${dish.id}`, axis, range);
    }
    if (dish.failure && dish.result === undefined) problems.push(`惩罚料理 ${dish.id} 没有 result`);
    if (dish.failure && !dish.failureCategory) problems.push(`惩罚料理 ${dish.id} 没有 failure_category`);
    if (!dish.failure && !dish.result) problems.push(`菜品 ${dish.id} 没有 result`);
}
for (const ingredient of data.INGREDIENTS) {
    for (const [axis, value] of Object.entries(ingredient.flavors || {})) {
        if (!data.FLAVORS.includes(axis)) problems.push(`食材 ${ingredient.id}: 未知味觉轴 ${axis}`);
        if (typeof value !== 'number' || value < 0) problems.push(`食材 ${ingredient.id}: ${axis} 数值非法 (${value})`);
    }
}

/* ---------------------------------------------------------------- 2. 引用检查 */

const knownModItems = new Set();
for (const item of data.DISH_ITEMS) knownModItems.add(`${NS}:${item.id}`);
// Java 侧 ModItems 注册的物品（从源码读，避免手工维护两份名单）
const modItemsSrc = fs.readFileSync(
    path.resolve(__dirname, '..', '..', 'src', 'main', 'java', 'com', 'qiyi', 'chinesedelight', 'item', 'ModItems.java'),
    'utf8');
{
    let m;
    const re = /ITEMS\.register(?:Simple)?Item\("([a-z_0-9]+)"|ITEMS\.registerItem\("([a-z_0-9]+)"/g;
    while ((m = re.exec(modItemsSrc)) !== null) knownModItems.add(`${NS}:${m[1] || m[2]}`);
}
for (const ingredient of data.INGREDIENTS) {
    const items = Array.isArray(ingredient.items) ? ingredient.items : [ingredient.items];
    for (const item of items) if (item.startsWith(`${NS}:`)) knownModItems.add(item);
}
// 自定义标签同样是合法的 must_have 引用目标
const knownTags = new Set();
for (const tag of data.TAGS || []) {
    knownModItems.add(`#${NS}:${tag.id}`);
    knownTags.add(`#${NS}:${tag.id}`);
}

function checkItemRef(where, ref) {
    if (typeof ref !== 'string') return;
    if (ref.startsWith('#')) {
        // 自定义标签必须真的定义过（原生标签如 #minecraft:fishes 放行）
        if (ref.startsWith(`#${NS}:`) && !knownTags.has(ref)) {
            warnings.push(`${where}: 引用了未定义的标签 ${ref}`);
        }
        return;
    }
    if (ref.startsWith(`${NS}:`) && !knownModItems.has(ref)) {
        warnings.push(`${where}: 引用了未登记的模组物品 ${ref}`);
    }
}

for (const dish of data.DISHES) {
    checkItemRef(`菜品 ${dish.id}`, dish.result);
    for (const entry of dish.mustHave || []) {
        const items = Array.isArray(entry.items) ? entry.items : [entry.items];
        items.forEach(item => checkItemRef(`菜品 ${dish.id} must_have`, item));
    }
}

// 每个菜品成品都要有贴图
const texDir = path.resolve(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', NS, 'textures', 'item');
for (const item of data.DISH_ITEMS) {
    const file = path.join(texDir, `${item.texture || item.id}.png`);
    if (!fs.existsSync(file)) warnings.push(`菜品成品 ${item.id}: 缺少贴图 textures/item/${item.texture || item.id}.png`);
}

/* ---------------------------------------------------------------- 2b. HolderSet 同质性
 *
 * must_have / must_not_have 的 items 走的是 RegistryCodecs.homogeneousList，
 * 它的 list 形态要求元素「同质」：要么全是物品 id，要么是单个 "#标签"。
 * 混着写（["#minecraft:fishes", "minecraft:cod"]）在加载世界时会抛
 * "Failed to parse ...json"，而且日志里只有一句笼统的错误，很难查。
 * 这里在生成阶段就拦住。
 */
function checkHomogeneous(where, items) {
    if (typeof items === 'string') return;          // 单个 id 或单个 "#标签" 都合法
    if (!Array.isArray(items)) {
        problems.push(`${where}: items 既不是字符串也不是数组`);
        return;
    }
    const hasTag = items.some(v => typeof v === 'string' && v.startsWith('#'));
    if (hasTag) {
        problems.push(
            `${where}: items 数组里混了 "#标签" 和物品 id，加载世界时会 Failed to parse。` +
            ` 请拆成独立的 must_have 条目，或改用自定义标签（见 data.js 的 TAGS）`);
    }
}

for (const dish of data.DISHES) {
    for (const entry of dish.mustHave || []) {
        checkHomogeneous(`菜品 ${dish.id} must_have`, entry.items);
    }
}
for (const recipe of data.FERMENTING) {
    for (const entry of recipe.ingredients) {
        checkHomogeneous(`陶缸 ${recipe.id}`, entry.items);
    }
}

/* ---------------------------------------------------------------- 2c. 物品模型齐全
 *
 * 物品栏里的每个物品都需要 assets/<ns>/models/item/<id>.json，否则进游戏会刷
 * "Unable to load model" 警告并且物品显示成紫黑格。
 * 方块物品（铁锅、石磨…）的模型指向 block 模型，这里只检查文件是否存在。
 */
const itemIdPattern = /ITEMS\.register(?:Simple)?Item\("([a-z_0-9]+)"|ITEMS\.registerItem\("([a-z_0-9]+)"/g;
const declaredItems = new Set();
for (const file of ['ModItems.java']) {
    const p = path.resolve(__dirname, '..', '..', 'src', 'main', 'java', 'com', 'qiyi', 'chinesedelight', 'item', file);
    if (!fs.existsSync(p)) continue;
    const text = fs.readFileSync(p, 'utf8');
    let m;
    while ((m = itemIdPattern.exec(text)) !== null) declaredItems.add(m[1] || m[2]);
}
// ModDishItems 的菜品物品（由 javagen.js 生成）
const dishItemsPath = path.resolve(__dirname, '..', '..', 'src', 'main', 'java', 'com', 'qiyi', 'chinesedelight', 'item', 'ModDishItems.java');
if (fs.existsSync(dishItemsPath)) {
    const text = fs.readFileSync(dishItemsPath, 'utf8');
    let m;
    const re = /new DishFood\("([a-z_0-9]+)"/g;
    while ((m = re.exec(text)) !== null) declaredItems.add(m[1]);
}
// 在 Java 里没有登记、但数据包里作为 result 用到的物品（例如已在别处注册的）
for (const item of data.DISH_ITEMS) declaredItems.add(item.id);

const modelDir = path.resolve(__dirname, '..', '..', 'src', 'main', 'resources', 'assets', NS, 'models', 'item');
for (const id of declaredItems) {
    if (!fs.existsSync(path.join(modelDir, `${id}.json`))) {
        warnings.push(`物品 ${id} 缺少模型 assets/${NS}/models/item/${id}.json（游戏里会显示成紫黑格）`);
    }
}

/* ---------------------------------------------------------------- 2d. 战利品表物品引用
 *
 * 方块掉落表里的 "name" 必须是真实存在的物品。改名/删物品时如果忘了改这里，
 * 游戏里方块就不掉东西（而且只在运行时才看得出来）。
 * 收一份「已知物品名」，把 loot_table 和自制配方里引用的模组物品都核一遍。
 */
const knownItems = new Set();
// Java 侧注册的
for (const f of ['ModItems.java', 'ModDishItems.java']) {
    const p = path.resolve(__dirname, '..', '..', 'src', 'main', 'java', 'com', 'qiyi', 'chinesedelight', 'item', f);
    if (!fs.existsSync(p)) continue;
    const text = fs.readFileSync(p, 'utf8');
    let m;
    const re = /(?:ITEMS\.register(?:Simple)?Item|ITEMS\.registerItem|new DishFood)\("([a-z_0-9]+)"/g;
    while ((m = re.exec(text)) !== null) knownItems.add(`${NS}:${m[1]}`);
}
// 方块物品
const blocksPath = path.resolve(__dirname, '..', '..', 'src', 'main', 'java', 'com', 'qiyi', 'chinesedelight', 'block', 'ModBlocks.java');
if (fs.existsSync(blocksPath)) {
    const text = fs.readFileSync(blocksPath, 'utf8');
    let m;
    const re = /(?:registerBlock|BLOCKS\.registerBlock)\("([a-z_0-9]+)"/g;
    while ((m = re.exec(text)) !== null) knownItems.add(`${NS}:${m[1]}`);
}
// 数据包里作为食材登记过的
for (const ingredient of data.INGREDIENTS) {
    const items = Array.isArray(ingredient.items) ? ingredient.items : [ingredient.items];
    for (const it of items) if (it.startsWith(`${NS}:`)) knownItems.add(it);
}
// 自定义标签也是合法的 must_have 引用目标
for (const tag of data.TAGS || []) knownItems.add(`#${NS}:${tag.id}`);
// 菜品成品
for (const item of data.DISH_ITEMS) knownItems.add(`${NS}:${item.id}`);

const lootDir = path.resolve(__dirname, '..', '..', 'src', 'main', 'resources', 'data', NS, 'loot_table');
if (fs.existsSync(lootDir)) {
    const walk = (dir) => {
        for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
            const p = path.join(dir, e.name);
            if (e.isDirectory()) { walk(p); continue; }
            if (!e.name.endsWith('.json')) continue;
            const text = fs.readFileSync(p, 'utf8');
            let m;
            const re = /"name"\s*:\s*"([a-z_0-9]+:[a-z_0-9_/]+)"/g;
            while ((m = re.exec(text)) !== null) {
                const ref = m[1];
                if (ref.startsWith(`${NS}:`) && !knownItems.has(ref)) {
                    problems.push(`战利品表 ${path.relative(path.resolve(__dirname, '..', '..'), p)} 引用了不存在的物品 ${ref}`);
                }
            }
        }
    };
    walk(lootDir);
}

/* ---------------------------------------------------------------- 3. 兜底 */

const active = data.DISHES.filter(dish => !dish.disabled);
if (!active.some(dish => dish.fallback)) problems.push('没有任何 fallback 兜底菜：配不出菜时玩家会什么都得不到');
if (!active.some(dish => dish.failure)) problems.push('没有任何惩罚料理');
if (!active.some(dish => dish.failure && dish.failureCategory === 'default')) {
    problems.push('缺少 failure_category = default 的惩罚料理（其它类别都不匹配时会用到）');
}

/* ---------------------------------------------------------------- 4. 采样模拟 */

function rangeOf(range) {
    if (typeof range === 'number') return { min: range, max: Infinity };
    return { min: range.min ?? 0, max: range.max ?? Infinity };
}

function inRange(value, range) {
    const { min, max } = rangeOf(range);
    return value >= min && value <= max;
}

// 「物品 id → {ref, ingredient}」，标签条目跳过（标签由 TAGS.members 单独展开）
const lookup = new Map();
for (const ingredient of data.INGREDIENTS) {
    const items = Array.isArray(ingredient.items) ? ingredient.items : [ingredient.items];
    for (const item of items) {
        if (!lookup.has(item)) lookup.set(item, ingredient);
    }
}

// 标签名 → 成员物品列表（生成期无法真解析标签，用 data.js 里显式声明的 members）
const tagMembers = new Map();
for (const tag of data.TAGS || []) {
    tagMembers.set(`#${NS}:${tag.id}`, tag.members);
    // 成员必须都有味道数据，否则在锅里算不出味道
    for (const member of tag.members) {
        if (!lookup.has(member)) {
            warnings.push(`标签 #${NS}:${tag.id} 的成员 ${member} 没有对应的食材味道数据`);
        }
    }
}

/** 把 must_have 的 items 规格展开成一批具体物品 id（标签用 members 代替）。 */
function expandWanted(spec) {
    const list = Array.isArray(spec) ? spec : [spec];
    const out = [];
    for (const v of list) {
        if (typeof v === 'string' && v.startsWith('#')) {
            const members = tagMembers.get(v);
            if (members) out.push(...members);
            // 未知标签（例如 #minecraft:fishes）无法展开，交给调用方兜底
        } else {
            out.push(v);
        }
    }
    return out;
}

const ingredients = [];
for (const [ref, ingredient] of lookup.entries()) {
    if (ref.startsWith('#')) continue;
    ingredients.push({ ref, ingredient });
}

function totalsOf(combo) {
    const totals = {};
    for (const { ingredient } of combo) {
        for (const [axis, value] of Object.entries(ingredient.flavors || {})) {
            totals[axis] = (totals[axis] || 0) + value;
        }
    }
    return totals;
}

function matchesDish(dish, combo, totals) {
    for (const [axis, range] of Object.entries(dish.flavors || {})) {
        if (!inRange(totals[axis] || 0, range)) return false;
    }
    const present = new Set(combo.map(c => c.ref));
    for (const entry of dish.mustHave || []) {
        const wanted = expandWanted(entry.items);
        const need = entry.min || 1;
        let found = 0;
        for (const ref of wanted) if (present.has(ref)) found++;
        if (found < need) return false;
    }
    return true;
}

const hits = new Map();
const matchedOnly = new Map();
active.forEach(dish => {
    hits.set(dish.id, 0);
    matchedOnly.set(dish.id, 0);
});

let seed = 20240925;
function random() {
    seed = (seed * 1103515245 + 12345) & 0x7fffffff;
    return seed / 0x7fffffff;
}
function pick(array) {
    return array[Math.floor(random() * array.length)];
}

function itemsMatching(spec) {
    const wanted = new Set(expandWanted(spec));
    return ingredients.filter(entry => wanted.has(entry.ref));
}

/** 和 Java 侧 CookingEvaluator 一样的判定，返回被选中的菜。 */
function choose(combo) {
    const totals = totalsOf(combo);
    const candidates = active.filter(dish => !dish.failure && matchesDish(dish, combo, totals));
    candidates.forEach(dish => matchedOnly.set(dish.id, matchedOnly.get(dish.id) + 1));
    const normal = candidates.filter(dish => !dish.fallback);
    const pool = normal.length > 0 ? normal : candidates;
    let chosen = null;
    for (const dish of pool) {
        if (!chosen || (dish.priority || 0) > (chosen.priority || 0)) chosen = dish;
    }
    if (chosen) return chosen;
    const failures = active.filter(dish => dish.failure);
    return failures.find(dish => dish.failureCategory === 'default') || failures[0] || null;
}

/* 随机采样：玩家乱丢东西时到底会不会出失败料理 */
const SAMPLES = 50000;
let failureHits = 0;
let emptyHits = 0;
for (let i = 0; i < SAMPLES; i++) {
    const size = 1 + Math.floor(random() * 4);
    const combo = [];
    for (let j = 0; j < size; j++) combo.push(pick(ingredients));
    const chosen = choose(combo);
    if (!chosen) {
        emptyHits++;
        continue;
    }
    if (chosen.failure) failureHits++;
    hits.set(chosen.id, hits.get(chosen.id) + 1);
}

/* 定向采样：每道菜按 must_have 拼一组，看它能不能做出来、能不能赢 */
const TARGETED = 3000;
for (const dish of active) {
    if (dish.failure) continue;
    const requirements = (dish.mustHave || [])
        .map(entry => itemsMatching(entry.items))
        .filter(list => list.length > 0);
    for (let i = 0; i < TARGETED; i++) {
        const combo = [];
        for (const options of requirements) {
            if (combo.length < 4) combo.push(pick(options));
        }
        while (combo.length < 4 && random() < 0.85) combo.push(pick(ingredients));
        if (combo.length === 0) continue;
        const chosen = choose(combo);
        if (chosen) hits.set(chosen.id, hits.get(chosen.id) + 1);
    }
}

/* 可行性判定：对没被选中的菜做确定性穷举，而不是靠随机采样猜。
 * （随机定向采样会给"条件很窄但确实可做"的菜报假警告，例如扬州炒饭。） */
function feasibleExample(dish) {
    const n = ingredients.length;
    const test = (combo) => matchesDish(dish, combo, totalsOf(combo)) ? combo.map(c => c.ref) : null;
    for (let a = 0; a < n; a++) {
        const one = [ingredients[a]];
        let r = test(one);
        if (r) return r;
        for (let b = a; b < n; b++) {
            const two = [ingredients[a], ingredients[b]];
            r = test(two);
            if (r) return r;
            for (let c = b; c < n; c++) {
                const three = [ingredients[a], ingredients[b], ingredients[c]];
                r = test(three);
                if (r) return r;
                for (let d = c; d < n; d++) {
                    const four = [ingredients[a], ingredients[b], ingredients[c], ingredients[d]];
                    r = test(four);
                    if (r) return r;
                }
            }
        }
    }
    return null;
}

for (const [id, count] of hits.entries()) {
    const dish = active.find(entry => entry.id === id);
    if (dish.failure) continue;
    if (count !== 0) continue;
    const example = feasibleExample(dish);
    if (!example) {
        warnings.push(`菜品 ${id}：穷举 1~4 格的所有组合都做不出来（条件互相矛盾）`);
    } else if (matchedOnly.get(id) === 0) {
        // 能匹配但采样没碰到，属于正常（条件较窄）
        continue;
    } else if (dish.fallback) {
        // 兜底菜被同名类的具体菜遮蔽是<b>设计意图</b>：兜底只是安全网，
        // 保证「以后有人改坏/删掉具体菜」时玩家仍能拿到合理产物。
        continue;
    } else {
        warnings.push(`菜品 ${id}：可做（例如 ${example.join(' + ')}）但一次都没赢过，被更高优先级的菜完全遮蔽`);
    }
}

/* ---------------------------------------------------------------- 输出 */

const normalDishes = active.filter(d => !d.failure).length;
console.log(`校验：食材 ${data.INGREDIENTS.length} 条，可做菜品 ${normalDishes} 道，惩罚料理 ${active.length - normalDishes} 道`);
console.log(`随机采样 ${SAMPLES} 次：出惩罚料理 ${failureHits} 次（${(failureHits / SAMPLES * 100).toFixed(2)}%），无结果 ${emptyHits} 次`);
console.log('\n命中次数（前 30）：');
for (const [id, count] of [...hits.entries()].sort((a, b) => b[1] - a[1]).slice(0, 30)) {
    console.log(`  ${id.padEnd(26)} ${count}`);
}

if (warnings.length > 0) {
    console.log('\n警告：');
    warnings.forEach(warning => console.log(`  ! ${warning}`));
}
if (problems.length > 0) {
    console.log('\n错误：');
    problems.forEach(problem => console.log(`  x ${problem}`));
    process.exit(1);
}
console.log('\n没有致命问题。');
