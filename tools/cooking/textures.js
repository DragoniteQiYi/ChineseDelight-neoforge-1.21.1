/*
 * 中华乐事 · 菜品贴图生成器
 * 用法：node tools/cooking/textures.js
 *
 * 生成 16x16 的像素风菜品图标到 assets/chinesedelight/textures/item/。
 * 每道菜用一种「形状 + 配色」描述，保证在物品栏里能互相区分。
 *
 * 只生成 data.js 里 DISH_ITEMS 中「贴图不存在」的那些，已有的不会被覆盖。
 */
const fs = require('fs');
const path = require('path');
const zlib = require('zlib');

const ROOT = path.resolve(__dirname, '..', '..');
const TEX = path.join(ROOT, 'src', 'main', 'resources', 'assets', 'chinesedelight', 'textures', 'item');

/* ---------------------------------------------------------------- PNG 编码 */

function crc32(buf) {
    let c, crc = 0xffffffff;
    for (let n = 0; n < buf.length; n++) {
        c = (crc ^ buf[n]) & 0xff;
        for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
        crc = c ^ (crc >>> 8);
    }
    return (crc ^ 0xffffffff) >>> 0;
}

function chunk(type, data) {
    const len = Buffer.alloc(4);
    len.writeUInt32BE(data.length, 0);
    const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
    const crc = Buffer.alloc(4);
    crc.writeUInt32BE(crc32(td), 0);
    return Buffer.concat([len, td, crc]);
}

/** pixels: 16x16 的 [r,g,b,a] 数组 */
function encodePng(pixels, size) {
    const raw = Buffer.alloc(size * (size * 4 + 1));
    let o = 0;
    for (let y = 0; y < size; y++) {
        raw[o++] = 0; // filter none
        for (let x = 0; x < size; x++) {
            const p = pixels[y * size + x];
            raw[o++] = p[0]; raw[o++] = p[1]; raw[o++] = p[2]; raw[o++] = p[3];
        }
    }
    const ihdr = Buffer.alloc(13);
    ihdr.writeUInt32BE(size, 0);
    ihdr.writeUInt32BE(size, 4);
    ihdr[8] = 8;   // bit depth
    ihdr[9] = 6;   // RGBA
    return Buffer.concat([
        Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]),
        chunk('IHDR', ihdr),
        chunk('IDAT', zlib.deflateSync(raw, { level: 9 })),
        chunk('IEND', Buffer.alloc(0)),
    ]);
}

/* ---------------------------------------------------------------- 调色板 */

const C = {
    clear: [0, 0, 0, 0],
    bowlDark: [72, 64, 56, 255],
    bowl: [168, 156, 138, 255],
    bowlLight: [206, 196, 180, 255],
    broth: [176, 122, 56, 255],
    brothLight: [206, 156, 84, 255],
    brothRed: [172, 62, 40, 255],
    red: [196, 58, 40, 255],
    redDark: [148, 36, 24, 255],
    green: [88, 152, 56, 255],
    greenDark: [58, 108, 36, 255],
    gold: [226, 176, 66, 255],
    goldDark: [186, 132, 36, 255],
    cream: [246, 238, 216, 255],
    white: [250, 248, 242, 255],
    brown: [134, 92, 52, 255],
    brownDark: [96, 62, 34, 255],
    meat: [150, 74, 60, 255],
    meatDark: [112, 52, 42, 255],
    pink: [226, 150, 150, 255],
    purple: [122, 74, 152, 255],
    dark: [54, 44, 38, 255],
    gray: [140, 140, 148, 255],
    yellow: [236, 208, 96, 255],
    orange: [222, 138, 48, 255],
};

/* ---------------------------------------------------------------- 画法 */

function blank() {
    return Array.from({ length: 16 * 16 }, () => [...C.clear]);
}

function put(px, x, y, col) {
    if (x < 0 || y < 0 || x > 15 || y > 15) return;
    px[y * 16 + x] = [...col];
}

/** 圆盘底（盘子/碗） */
function plate(px, rim = C.bowlLight, body = C.bowl) {
    for (let y = 3; y <= 14; y++) {
        for (let x = 1; x <= 14; x++) {
            const dx = (x - 7.5) / 6.6, dy = (y - 9) / 5.2;
            const d = dx * dx + dy * dy;
            if (d <= 1) put(px, x, y, d > 0.74 ? rim : body);
        }
    }
}

/** 碗 + 汤 */
function soup(px, broth = C.broth, top = C.brothLight) {
    for (let y = 6; y <= 14; y++) {
        for (let x = 1; x <= 14; x++) {
            const dx = (x - 7.5) / 6.8, dy = (y - 10) / 4.6;
            const d = dx * dx + dy * dy;
            if (d <= 1) put(px, x, y, d > 0.72 ? C.bowlLight : C.bowl);
        }
    }
    for (let y = 5; y <= 9; y++) {
        for (let x = 2; x <= 13; x++) {
            const dx = (x - 7.5) / 5.6, dy = (y - 7) / 2.4;
            if (dx * dx + dy * dy <= 1) put(px, x, y, y <= 6 ? top : broth);
        }
    }
}

/** 一堆块状物（炒菜） */
function mound(px, cols, seed) {
    let s = seed || 1;
    const rnd = () => (s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
    plate(px, C.bowlLight, C.bowlDark);
    for (let y = 4; y <= 11; y++) {
        for (let x = 2; x <= 13; x++) {
            const dx = (x - 7.5) / 5.6, dy = (y - 8) / 4.0;
            if (dx * dx + dy * dy <= 1 && rnd() > 0.14) {
                put(px, x, y, cols[Math.floor(rnd() * cols.length)]);
            }
        }
    }
}

/** 细条状（土豆丝、面条） */
function strands(px, cols, vertical, seed) {
    let s = seed || 7;
    const rnd = () => (s = (s * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff;
    plate(px, C.bowlLight, C.bowlDark);
    for (let i = 0; i < 9; i++) {
        const col = cols[i % cols.length];
        const base = 3 + i;
        const len = 6 + Math.floor(rnd() * 5);
        for (let j = 0; j < len; j++) {
            if (vertical) put(px, base, 4 + j, col);
            else put(px, 3 + j, base - 1, col);
        }
    }
}

/** 半圆形（饺子、馒头） */
function dome(px, body, shade, baseY = 13) {
    for (let y = 3; y <= baseY; y++) {
        for (let x = 1; x <= 14; x++) {
            const dx = (x - 7.5) / 6.2, dy = (y - baseY) / 8.0;
            if (dx * dx + dy * dy <= 1 && y <= baseY) put(px, x, y, y > baseY - 3 ? shade : body);
        }
    }
}

/** 扁平圆饼（葱油饼） */
function disc(px, body, shade, top) {
    for (let y = 4; y <= 13; y++) {
        for (let x = 1; x <= 14; x++) {
            const dx = (x - 7.5) / 6.4, dy = (y - 8.5) / 4.6;
            if (dx * dx + dy * dy <= 1) put(px, x, y, y > 10 ? shade : body);
        }
    }
    for (let i = 0; i < 7; i++) put(px, 4 + i, 6 + (i % 2), top);
    for (let i = 0; i < 6; i++) put(px, 5 + i, 9 - (i % 2), top);
}

/** 一条鱼 */
function fish(px, body, shade) {
    for (let y = 6; y <= 11; y++) {
        for (let x = 2; x <= 12; x++) {
            const dy = (y - 8.5) / 2.6;
            if (dy * dy <= 1 - Math.pow((x - 7) / 7.0, 2)) put(px, x, y, y >= 9 ? shade : body);
        }
    }
    put(px, 12, 7, shade); put(px, 13, 6, shade); put(px, 13, 11, shade); put(px, 12, 10, shade);
    put(px, 3, 8, C.dark);
}

/** 蛋饼（圆形带蛋黄） */
function friedEgg(px, white = C.white, yolk = C.gold) {
    for (let y = 3; y <= 13; y++) {
        for (let x = 1; x <= 14; x++) {
            const dx = (x - 7.5) / 6.4, dy = (y - 8) / 5.0;
            if (dx * dx + dy * dy <= 1) put(px, x, y, white);
        }
    }
    for (let y = 6; y <= 10; y++) {
        for (let x = 6; x <= 10; x++) {
            const dx = x - 8, dy = y - 8;
            if (dx * dx + dy * dy <= 3.2) put(px, x, y, yolk);
        }
    }
}

/* ---------------------------------------------------------------- 菜谱：形状 + 配色 */

const RECIPES = {
    scallion_egg: g => mound(g, [C.gold, C.yellow, C.green, C.greenDark], 11),
    steamed_egg: g => { plate(g, C.cream, C.gold); for (let y = 5; y <= 10; y++) for (let x = 4; x <= 11; x++) { const dx = (x - 7.5) / 3.6, dy = (y - 7.5) / 2.8; if (dx * dx + dy * dy <= 1) put(g, x, y, C.yellow); } },
    spicy_tofu: g => mound(g, [C.red, C.redDark, C.cream, C.green], 3),
    home_style_tofu: g => mound(g, [C.cream, C.gold, C.green, C.brown], 5),
    mushroom_tofu: g => mound(g, [C.cream, C.brown, C.brownDark, C.green], 9),
    cabbage_tofu_soup: g => soup(g, C.brothLight, C.cream),
    edamame_dish: g => mound(g, [C.green, C.greenDark, C.green], 13),
    dried_tofu_stirfry: g => mound(g, [C.brown, C.gold, C.greenDark, C.brownDark], 17),
    garlic_vegetable: g => mound(g, [C.green, C.greenDark, C.cream, C.green], 19),
    stir_fried_greens: g => mound(g, [C.green, C.greenDark, C.green, C.gold], 23),
    cabbage_stirfry: g => mound(g, [C.cream, C.green, C.white, C.greenDark], 29),
    potato_shreds: g => strands(g, [C.yellow, C.gold, C.cream], false, 31),
    sour_potato_shreds: g => strands(g, [C.yellow, C.red, C.orange], false, 37),
    mushroom_stirfry: g => mound(g, [C.brown, C.brownDark, C.green, C.brown], 41),
    fish_fragrant_pork: g => strands(g, [C.meat, C.orange, C.green, C.red], false, 43),
    twice_cooked_pork: g => mound(g, [C.meat, C.meatDark, C.green, C.red], 47),
    braised_pork: g => mound(g, [C.meatDark, C.meat, C.brown, C.gold], 53),
    beef_potato_stew: g => mound(g, [C.meatDark, C.yellow, C.brown, C.orange], 59),
    chili_pork: g => mound(g, [C.meat, C.red, C.green, C.redDark], 61),
    scallion_beef: g => mound(g, [C.meatDark, C.green, C.meat, C.greenDark], 67),
    cumin_mutton: g => mound(g, [C.meat, C.brownDark, C.gold, C.meatDark], 71),
    chicken_mushroom: g => mound(g, [C.cream, C.brown, C.gold, C.brownDark], 73),
    ginger_chicken: g => mound(g, [C.cream, C.gold, C.green, C.orange], 79),
    meat_stirfry: g => mound(g, [C.meat, C.meatDark, C.green, C.brown], 83),
    steamed_fish: g => fish(g, C.gray, C.cream),
    braised_fish: g => fish(g, C.brown, C.brownDark),
    yangzhou_fried_rice: g => mound(g, [C.white, C.gold, C.green, C.orange], 89),
    egg_fried_rice: g => mound(g, [C.white, C.yellow, C.gold, C.cream], 97),
    beef_noodle_soup: g => { soup(g, C.broth, C.brothLight); strands(g, [C.cream, C.yellow], false, 101); for (let i = 0; i < 4; i++) put(g, 5 + i, 6, C.meatDark); },
    scallion_oil_noodles: g => { plate(g, C.bowlLight, C.bowlDark); strands(g, [C.gold, C.goldDark, C.yellow], false, 103); for (let i = 0; i < 5; i++) put(g, 5 + i, 7, C.greenDark); },
    fried_noodles: g => { plate(g, C.bowlLight, C.bowlDark); strands(g, [C.gold, C.orange, C.green, C.yellow], false, 107); },
    sour_noodles: g => { soup(g, C.brothRed, C.red); strands(g, [C.cream, C.yellow], false, 109); },
    dumpling: g => { dome(g, C.cream, C.white, 12); for (let i = 0; i < 5; i++) put(g, 4 + i * 2, 9, C.bowlLight); },
    scallion_pancake: g => disc(g, C.gold, C.goldDark, C.greenDark),
    egg_drop_soup: g => { soup(g, C.brothLight, C.cream); for (let i = 0; i < 6; i++) put(g, 4 + i, 7 + (i % 2), C.yellow); },
    hot_sour_soup: g => { soup(g, C.brothRed, C.redDark); for (let i = 0; i < 5; i++) put(g, 5 + i, 6 + (i % 2), C.orange); },
    kelp_soup: g => { soup(g, C.greenDark, C.green); for (let i = 0; i < 6; i++) put(g, 4 + i, 7, C.greenDark); },
    mushroom_soup: g => { soup(g, C.brown, C.brownDark); for (let i = 0; i < 5; i++) put(g, 5 + i, 6 + (i % 2), C.brownDark); },
    cold_kelp: g => mound(g, [C.greenDark, C.green, C.cream, C.greenDark], 113),
    smoked_fish: g => fish(g, C.orange, C.brownDark),
    // 兜底产物
    spiced_mix: g => mound(g, [C.red, C.orange, C.brownDark, C.redDark], 127),
    plain_mix: g => mound(g, [C.gray, C.cream, C.gold, C.brown], 131),
};

/* ---------------------------------------------------------------- 主流程 */

const data = require('./data');
fs.mkdirSync(TEX, { recursive: true });

let made = 0, skipped = 0, unknown = [];
const noRecipe = [];
for (const item of data.DISH_ITEMS) {
    const name = item.texture || item.id;
    const file = path.join(TEX, `${name}.png`);
    if (fs.existsSync(file)) { skipped++; continue; }
    const recipe = RECIPES[item.id];
    if (!recipe) { unknown.push(item.id); continue; }
    const px = blank();
    recipe(px);
    fs.writeFileSync(file, encodePng(px, 16));
    made++;
}

console.log(`贴图：新生成 ${made} 张，已存在跳过 ${skipped} 张`);
if (unknown.length) console.log(`没有画法定义的成品（${unknown.length}）：${unknown.join(', ')}`);
