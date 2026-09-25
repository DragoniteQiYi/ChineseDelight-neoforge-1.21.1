/*
 * 中华乐事 · 内容生成器（1.21.1 版）
 * 用法：node tools/cooking/generate.js
 *
 * 读取 data.js（唯一真相来源），生成：
 *   - 数据包注册表：data/chinesedelight/chinesedelight/{ingredient,dish,grinding,fermenting}/*.json
 *   - 菜品成品的物品模型：assets/chinesedelight/models/item/*.json
 *
 * 注意：
 *   - 1.21.1 的物品模型就是 models/item/<id>.json，没有 1.21.5 的 assets/<ns>/items/ 定义文件。
 *   - 语言文件不在这里生成，避免覆盖手写内容；新增菜名的词条取自 DISH_ITEMS 的 zh/en。
 */
const fs = require('fs');
const path = require('path');
const data = require('./data');

const ROOT = path.resolve(__dirname, '..', '..');
const RES = path.join(ROOT, 'src', 'main', 'resources');
const NS = data.NS;

let written = 0;

function writeJson(rel, value) {
    const file = path.join(RES, rel);
    fs.mkdirSync(path.dirname(file), { recursive: true });
    fs.writeFileSync(file, JSON.stringify(value, null, 2) + '\n');
    written++;
}

/* ---------------------------------------------------------------- 数据包注册表 */

function flavorMap(flavors) {
    const out = {};
    for (const [axis, value] of Object.entries(flavors || {})) out[axis] = value;
    return out;
}

// 食材
for (const ingredient of data.INGREDIENTS) {
    if (ingredient.disabled) continue;
    const json = {
        items: ingredient.items,
        flavors: flavorMap(ingredient.flavors),
    };
    if (ingredient.failureCategory) json.failure_category = ingredient.failureCategory;
    writeJson(`data/${NS}/${NS}/ingredient/${ingredient.id}.json`, json);
}

// 菜品
for (const dish of data.DISHES) {
    if (dish.disabled) continue;
    const json = {};
    if (dish.priority !== undefined) json.priority = dish.priority;
    if (dish.station) json.station = dish.station;
    if (dish.flavors) json.flavors = flavorMap(dish.flavors);
    if (dish.mustHave) {
        json.must_have = dish.mustHave.map(entry => ({
            items: entry.items,
            ...(entry.min && entry.min !== 1 ? { min: entry.min } : {}),
        }));
    }
    if (dish.mustNotHave) json.must_not_have = dish.mustNotHave;
    json.result = { id: dish.result, count: dish.count || 1 };
    if (dish.fallback) json.fallback = true;
    if (dish.failure) json.failure = true;
    if (dish.failureCategory) json.failure_category = dish.failureCategory;
    writeJson(`data/${NS}/${NS}/dish/${dish.id}.json`, json);
}

// 石磨
for (const recipe of data.GRINDING) {
    writeJson(`data/${NS}/${NS}/grinding/${recipe.id}.json`, {
        input: recipe.input,
        water: !!recipe.water,
        time: recipe.time,
        output_mode: recipe.outputMode || 'item',
        result: typeof recipe.result === 'string' ? { id: recipe.result, count: 1 } : recipe.result,
    });
}

// 陶缸
for (const recipe of data.FERMENTING) {
    writeJson(`data/${NS}/${NS}/fermenting/${recipe.id}.json`, {
        ingredients: recipe.ingredients.map(entry => ({
            items: entry.items,
            ...(entry.min && entry.min !== 1 ? { min: entry.min } : {}),
        })),
        water: !!recipe.water,
        time: recipe.time,
        result: typeof recipe.result === 'string' ? { id: recipe.result, count: 1 } : recipe.result,
    });
}

/* ---------------------------------------------------------------- 标签 */

for (const tag of data.TAGS) {
    writeJson(`data/${NS}/tags/item/${tag.id}.json`, {
        replace: false,
        values: tag.values,
    });
}

/* ---------------------------------------------------------------- 菜品成品模型 */

for (const item of data.DISH_ITEMS) {
    // existing 的物品已在 ModItems 注册，模型也由那边提供，不要重复生成
    if (item.existing) continue;
    writeJson(`assets/${NS}/models/item/${item.id}.json`, {
        parent: 'minecraft:item/generated',
        textures: { layer0: `${NS}:item/${item.texture || item.id}` },
    });
}

// 已由 ModItems 注册、但仍需物品模型的条目
for (const id of data.EXTRA_ITEM_MODELS || []) {
    writeJson(`assets/${NS}/models/item/${id}.json`, {
        parent: 'minecraft:item/generated',
        textures: { layer0: `${NS}:item/${id}` },
    });
}

console.log(`generated ${written} files from data.js`);
console.log(`  食材 ${data.INGREDIENTS.length} 条 / 菜品 ${data.DISHES.length} 道 / 石磨 ${data.GRINDING.length} 条 / 陶缸 ${data.FERMENTING.length} 条 / 成品 ${data.DISH_ITEMS.length} 个`);

const missing = data.DISH_ITEMS
    .map(item => `${item.texture || item.id}.png`)
    .filter(name => !fs.existsSync(path.join(RES, 'assets', NS, 'textures', 'item', name)));
if (missing.length > 0) {
    console.log(`\n缺少贴图的菜品成品（${missing.length}）：`);
    missing.forEach(name => console.log(`  ! textures/item/${name}`));
}
