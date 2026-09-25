/*
 * 中华乐事 · 物品/方块注册冲突检查
 * 用法：node tools/cooking/checkreg.js
 *
 * 钢性检查「同一个注册名被注册两次」。这类错误只在启动游戏时才会暴露，
 * 报错形式是 IllegalArgumentException: Duplicate registration <name>，排查很费时间。
 * 这里直接扫 Java 源码，把冲突在编译前就找出来。
 *
 * 覆盖的注册写法：
 *   ModItems:  ITEMS.registerItem("x" ...) / registerSimpleItem("x")
 *   ModDishItems: dishes.add(new DishFood("x" ...))   ← 由 javagen.js 生成
 *   方块:      BLOCKS.registerBlock("x" ...) / registerSimpleBlock
 */
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..', '..');
const JAVA = path.join(ROOT, 'src', 'main', 'java', 'com', 'qiyi', 'chinesedelight');

function javaFiles(dir) {
    const out = [];
    for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
        const p = path.join(dir, entry.name);
        if (entry.isDirectory()) out.push(...javaFiles(p));
        else if (entry.name.endsWith('.java')) out.push(p);
    }
    return out;
}

/** 收集「注册名 -> 来源列表」 */
const registrations = new Map();
function record(name, where) {
    if (!registrations.has(name)) registrations.set(name, []);
    registrations.get(name).push(where);
}

const PATTERNS = [
    // 物品
    [/\bITEMS\.registerItem\("([a-z_0-9]+)"/g, 'ModItems.registerItem'],
    [/\bITEMS\.registerSimpleItem\("([a-z_0-9]+)"/g, 'ModItems.registerSimpleItem'],
    // ModDishItems 生成的条目
    [/new DishFood\("([a-z_0-9]+)"/g, 'ModDishItems'],
    // 方块（方块与物品同名时通常成对出现，不算冲突——分开统计）
    [/\bBLOCKS\.registerBlock\("([a-z_0-9]+)"/g, 'ModBlocks.registerBlock'],
];

const blockNames = new Set();

for (const file of javaFiles(JAVA)) {
    const rel = path.relative(ROOT, file).replace(/\\/g, '/');
    const text = fs.readFileSync(file, 'utf8');
    for (const [re, label] of PATTERNS) {
        re.lastIndex = 0;
        let m;
        while ((m = re.exec(text)) !== null) {
            if (label === 'ModBlocks.registerBlock') blockNames.add(m[1]);
            else record(m[1], `${label} @ ${rel}`);
        }
    }
}

/* 物品与方块同名是合法的（方块物品），真正的冲突是「物品注册两次」 */
const duplicates = [...registrations.entries()].filter(([, list]) => list.length > 1);
if (duplicates.length > 0) {
    console.log(`发现 ${duplicates.length} 个重复的物品注册名（会导致启动时 Duplicate registration）：`);
    for (const [name, list] of duplicates) {
        console.log(`  x ${name}`);
        list.forEach(w => console.log(`      <- ${w}`));
    }
    process.exit(1);
}

console.log(`注册名检查通过：${registrations.size} 个物品名，无重复。`);
console.log(`  （另有 ${blockNames.size} 个方块名；方块与物品同名属正常，未计入重复）`);
