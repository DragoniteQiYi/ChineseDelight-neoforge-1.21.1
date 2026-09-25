/*
 * 中华乐事 · 烹饪数据总表（唯一真相来源）
 *
 * 改数值、加菜、加食材都只改这个文件，然后跑：
 *     node tools/cooking/generate.js   # 生成数据包 JSON、物品模型
 *     node tools/cooking/validate.js   # 检查区间合法 / 菜被遮蔽 / 缺兜底 / 失败率
 *
 * 味觉轴：vegetable 蔬菜 / sweet 甜 / salty 咸 / sour 酸 / bitter 苦 / spicy 辣 /
 *        umami 鲜 / numbing 麻 / oily 油 / starchy 淀粉
 *
 * 配菜原则（为了让铁锅「少出失败料理」）：
 *   1. 肉 / 鱼 / 蛋 / 豆腐 / 主食 各有自己的菜路，随便丢也能落到某道菜上；
 *   2. 优先级高的菜条件更严，优先级低的更宽松，最后有兜底菜接住剩余组合；
 *   3. 兜底菜条件刻意写得很松，保证「乱配也能出菜」。
 */

const NS = 'chinesedelight';

const FLAVORS = ['vegetable', 'sweet', 'salty', 'sour', 'bitter', 'spicy', 'umami', 'numbing', 'oily', 'starchy'];

const FLAVOR_NAMES = {
    vegetable: ['蔬菜', 'Vegetable'],
    sweet: ['甜', 'Sweet'],
    salty: ['咸', 'Salty'],
    sour: ['酸', 'Sour'],
    bitter: ['苦', 'Bitter'],
    spicy: ['辣', 'Spicy'],
    umami: ['鲜', 'Umami'],
    numbing: ['麻', 'Numbing'],
    oily: ['油', 'Oily'],
    starchy: ['淀粉', 'Starchy'],
};

/** 食材：items 可以写物品 id、id 数组，或者标签（"#minecraft:fishes"）。 */
const INGREDIENTS = [
    // ---- 原版蔬菜 ----
    { id: 'carrot', items: 'minecraft:carrot', flavors: { vegetable: 2, sweet: 1 } },
    { id: 'potato', items: 'minecraft:potato', flavors: { vegetable: 2, starchy: 2 } },
    { id: 'baked_potato', items: 'minecraft:baked_potato', flavors: { vegetable: 1, starchy: 3 } },
    { id: 'beetroot', items: 'minecraft:beetroot', flavors: { vegetable: 2, sweet: 1 } },
    { id: 'kelp', items: 'minecraft:kelp', flavors: { salty: 2, umami: 1, vegetable: 1 }, failureCategory: 'fish' },
    { id: 'dried_kelp', items: 'minecraft:dried_kelp', flavors: { salty: 3, umami: 2 }, failureCategory: 'fish' },
    // ---- 原版肉类 ----
    { id: 'beef', items: 'minecraft:beef', flavors: { umami: 3, oily: 1 }, failureCategory: 'meat' },
    { id: 'cooked_beef', items: 'minecraft:cooked_beef', flavors: { umami: 4, oily: 2 }, failureCategory: 'meat' },
    { id: 'porkchop', items: 'minecraft:porkchop', flavors: { umami: 2, oily: 1 }, failureCategory: 'meat' },
    { id: 'cooked_porkchop', items: 'minecraft:cooked_porkchop', flavors: { umami: 3, oily: 2 }, failureCategory: 'meat' },
    { id: 'chicken', items: 'minecraft:chicken', flavors: { umami: 2 }, failureCategory: 'meat' },
    { id: 'cooked_chicken', items: 'minecraft:cooked_chicken', flavors: { umami: 3 }, failureCategory: 'meat' },
    { id: 'mutton', items: 'minecraft:mutton', flavors: { umami: 2, oily: 1 }, failureCategory: 'meat' },
    { id: 'cooked_mutton', items: 'minecraft:cooked_mutton', flavors: { umami: 3, oily: 1 }, failureCategory: 'meat' },
    { id: 'rotten_flesh', items: 'minecraft:rotten_flesh', flavors: { umami: 1, bitter: 1 }, failureCategory: 'meat' },
    // ---- 原版鱼 ----
    { id: 'cod', items: 'minecraft:cod', flavors: { umami: 2 }, failureCategory: 'fish' },
    { id: 'cooked_cod', items: 'minecraft:cooked_cod', flavors: { umami: 3 }, failureCategory: 'fish' },
    { id: 'salmon', items: 'minecraft:salmon', flavors: { umami: 2, oily: 1 }, failureCategory: 'fish' },
    { id: 'cooked_salmon', items: 'minecraft:cooked_salmon', flavors: { umami: 3, oily: 1 }, failureCategory: 'fish' },
    { id: 'tropical_fish', items: 'minecraft:tropical_fish', flavors: { umami: 1 }, failureCategory: 'fish' },
    { id: 'pufferfish', items: 'minecraft:pufferfish', flavors: { umami: 2, bitter: 1 }, failureCategory: 'fish' },
    // ---- 原版蛋 / 主食 / 蘑菇 ----
    { id: 'egg', items: 'minecraft:egg', flavors: { umami: 1 } },
    { id: 'wheat', items: 'minecraft:wheat', flavors: { starchy: 2 } },
    { id: 'bread', items: 'minecraft:bread', flavors: { starchy: 3, sweet: 1 } },
    { id: 'brown_mushroom', items: 'minecraft:brown_mushroom', flavors: { umami: 2, vegetable: 1 } },
    { id: 'red_mushroom', items: 'minecraft:red_mushroom', flavors: { umami: 2, vegetable: 1 } },
    // ---- 原版甜味 / 其它 ----
    { id: 'apple', items: 'minecraft:apple', flavors: { sweet: 2, sour: 1 } },
    { id: 'sweet_berries', items: 'minecraft:sweet_berries', flavors: { sweet: 2, sour: 1 } },
    { id: 'glow_berries', items: 'minecraft:glow_berries', flavors: { sweet: 2 } },
    { id: 'melon_slice', items: 'minecraft:melon_slice', flavors: { sweet: 2, vegetable: 1 } },
    { id: 'sugar', items: 'minecraft:sugar', flavors: { sweet: 3 } },
    { id: 'honey_bottle', items: 'minecraft:honey_bottle', flavors: { sweet: 4 } },
    { id: 'milk_bucket', items: 'minecraft:milk_bucket', flavors: { sweet: 1, oily: 1 } },
    { id: 'mushroom_stew', items: 'minecraft:mushroom_stew', flavors: { umami: 2, vegetable: 2 } },
    { id: 'beetroot_soup', items: 'minecraft:beetroot_soup', flavors: { vegetable: 2, sweet: 1 } },
    { id: 'cookie', items: 'minecraft:cookie', flavors: { sweet: 3, starchy: 1 } },
    { id: 'pumpkin_pie', items: 'minecraft:pumpkin_pie', flavors: { sweet: 3, starchy: 1 } },
    // 通用标签兜底：其它模组的食物也能下锅
    { id: 'common_fishes', items: '#minecraft:fishes', flavors: { umami: 1 }, failureCategory: 'fish' },

    // ---- 中华乐事自己的作物与加工品 ----
    { id: 'smallscallion', items: `${NS}:smallscallion`, flavors: { vegetable: 1, umami: 1 } },
    { id: 'bigscallion', items: `${NS}:bigscallion`, flavors: { vegetable: 2, umami: 1 } },
    { id: 'ginger', items: `${NS}:ginger`, flavors: { spicy: 1, umami: 1 } },
    { id: 'garlic', items: `${NS}:garlic`, flavors: { spicy: 1, umami: 1 } },
    { id: 'garlicsprout', items: `${NS}:garlicsprout`, flavors: { vegetable: 2, umami: 1 } },
    { id: 'garlicscape', items: `${NS}:garlicscape`, flavors: { vegetable: 2, umami: 1 } },
    { id: 'napacabbage', items: `${NS}:napacabbage`, flavors: { vegetable: 3 } },
    { id: 'pickledcabbage', items: `${NS}:pickledcabbage`, flavors: { vegetable: 1, sour: 3, salty: 1 } },
    { id: 'soybean', items: `${NS}:soybean`, flavors: { vegetable: 1, umami: 1, starchy: 1 } },
    { id: 'edamame', items: `${NS}:edamame`, flavors: { vegetable: 2, umami: 1 } },
    { id: 'cookingoil', items: `${NS}:cookingoil`, flavors: { oily: 3 } },
    { id: 'cookingsalt', items: `${NS}:cookingsalt`, flavors: { salty: 3 } },
    { id: 'soysauce', items: `${NS}:soysauce`, flavors: { salty: 2, umami: 2 } },
    { id: 'vinegar', items: `${NS}:vinegar`, flavors: { sour: 3 } },
    { id: 'duckegg', items: `${NS}:duckegg`, flavors: { umami: 1 } },
    { id: 'saltedduckegg', items: `${NS}:saltedduckegg`, flavors: { salty: 2, umami: 2 } },
    { id: 'omelet', items: `${NS}:omelet`, flavors: { umami: 2, oily: 1, starchy: 1 } },
    { id: 'riceporridge', items: `${NS}:riceporridge`, flavors: { starchy: 3, umami: 1 } },
    { id: 'soymilk', items: `${NS}:soymilk_bucket`, flavors: { umami: 1, sweet: 1 } },
    { id: 'cookedsoymilk', items: `${NS}:cookedsoymilk_bucket`, flavors: { umami: 2, sweet: 1 } },
    { id: 'yogurt', items: `${NS}:yogurt_bucket`, flavors: { sweet: 2, sour: 1 } },
    { id: 'tofu', items: `${NS}:tofu`, flavors: { umami: 2, vegetable: 1 } },
    { id: 'driedtofu', items: `${NS}:driedtofu`, flavors: { umami: 3, salty: 1 } },
    { id: 'doubanjiang', items: `${NS}:doubanjiang`, flavors: { spicy: 2, salty: 2, umami: 2 } },
    { id: 'noodles', items: `${NS}:noodles`, flavors: { starchy: 3 } },
    { id: 'steamedbun', items: `${NS}:steamedbun`, flavors: { starchy: 3, sweet: 1 } },
    { id: 'flour', items: `${NS}:flour`, flavors: { starchy: 2 } },
    { id: 'dough', items: `${NS}:dough`, flavors: { starchy: 2 } },
    { id: 'starch', items: `${NS}:starch`, flavors: { starchy: 3 } },
    { id: 'chilipowder', items: `${NS}:chilipowder`, flavors: { spicy: 3 } },
    { id: 'red_chilli', items: `${NS}:red_chilli`, flavors: { spicy: 2, vegetable: 1 } },
    { id: 'peppercorn', items: `${NS}:peppercorn`, flavors: { numbing: 3 } },
];

/*
 * 菜品。判定顺序：
 *   1. 硬性条件（station / flavors 区间 / must_have / must_not_have）
 *   2. 普通菜里 priority 最大的
 *   3. 没有普通菜时才看 fallback 兜底菜
 *   4. 还是没有就按 failure_category 出惩罚料理
 *
 * result 是物品 id；多道菜可以共用同一个成品（例如各种炒青菜都出「炒时蔬」）。
 */
const DISHES = [
    /* ================= 蛋类 ================= */
    {
        id: 'garlic_sprout_omelet', priority: 30, station: 'iron_pot',
        flavors: { umami: { min: 2 }, oily: { min: 1 }, vegetable: { min: 1 } },
        mustHave: [{ items: 'minecraft:egg' }, { items: `${NS}:garlicsprout` }],
        result: `${NS}:omelet`,
    },
    {
        id: 'scallion_egg', priority: 26, station: 'iron_pot',
        flavors: { umami: { min: 2 }, oily: { min: 1 } },
        mustHave: [{ items: 'minecraft:egg' }, { items: `${NS}:bigscallion` }],
        result: `${NS}:scallion_egg`,
    },
    {
        id: 'steamed_egg', priority: 22, station: 'iron_pot',
        flavors: { umami: { min: 2 } },
        mustHave: [{ items: 'minecraft:egg' }, { items: `${NS}:soysauce` }],
        result: `${NS}:steamed_egg`,
    },
    {
        id: 'egg_omelet', priority: 12, station: 'iron_pot',
        flavors: { umami: { min: 1 }, oily: { min: 1 } },
        mustHave: [{ items: 'minecraft:egg' }],
        result: `${NS}:omelet`,
    },

    /* ================= 豆腐 ================= */
    {
        id: 'mapo_tofu', priority: 40, station: 'iron_pot',
        flavors: { spicy: { min: 2, max: 8 }, umami: { min: 2 }, oily: { min: 1 }, sweet: { max: 2 } },
        mustHave: [{ items: `${NS}:tofu` }, { items: `${NS}:doubanjiang` }],
        result: `${NS}:mapo_tofu`,
    },
    {
        id: 'spicy_tofu', priority: 34, station: 'iron_pot',
        flavors: { spicy: { min: 2 }, umami: { min: 2 } },
        mustHave: [{ items: `${NS}:tofu` }, { items: `${NS}:red_chilli` }],
        result: `${NS}:spicy_tofu`,
    },
    {
        id: 'home_style_tofu', priority: 24, station: 'iron_pot',
        flavors: { umami: { min: 3 }, salty: { min: 1 } },
        mustHave: [{ items: `${NS}:tofu` }, { items: `${NS}:soysauce` }],
        result: `${NS}:home_style_tofu`,
    },
    {
        id: 'mushroom_tofu', priority: 20, station: 'iron_pot',
        flavors: { umami: { min: 3 }, vegetable: { min: 1 } },
        mustHave: [
            { items: ['minecraft:brown_mushroom', 'minecraft:red_mushroom'] },
            { items: `${NS}:tofu` },
        ],
        result: `${NS}:mushroom_tofu`,
    },
    {
        id: 'cabbage_tofu_soup', priority: 22, station: 'iron_pot',
        flavors: { vegetable: { min: 2 }, umami: { min: 2 } },
        mustHave: [{ items: `${NS}:napacabbage` }, { items: `${NS}:tofu` }],
        result: `${NS}:cabbage_tofu_soup`,
    },
    {
        id: 'tofu_dish', priority: 10, station: 'iron_pot',
        flavors: { umami: { min: 1 } },
        mustHave: [{ items: `${NS}:tofu` }],
        result: `${NS}:home_style_tofu`,
    },

    /* ================= 豆制品 ================= */
    {
        id: 'edamame_dish', priority: 14, station: 'iron_pot',
        flavors: { vegetable: { min: 2 } },
        mustHave: [{ items: `${NS}:edamame` }],
        result: `${NS}:edamame_dish`,
    },
    {
        id: 'dried_tofu_stirfry', priority: 22, station: 'iron_pot',
        flavors: { umami: { min: 3 }, oily: { min: 1 } },
        mustHave: [{ items: `${NS}:driedtofu` }],
        result: `${NS}:dried_tofu_stirfry`,
    },

    /* ================= 蔬菜 ================= */
    {
        id: 'sour_potato_shreds', priority: 26, station: 'iron_pot',
        flavors: { starchy: { min: 3 }, sour: { min: 2 }, spicy: { min: 1 } },
        mustHave: [
            { items: ['minecraft:potato', 'minecraft:baked_potato'] },
            { items: [`${NS}:red_chilli`, `${NS}:chilipowder`] },
        ],
        result: `${NS}:sour_potato_shreds`,
    },
    {
        id: 'potato_shreds', priority: 24, station: 'iron_pot',
        flavors: { starchy: { min: 3 }, oily: { min: 1 }, sour: { min: 1 } },
        mustHave: [
            { items: ['minecraft:potato', 'minecraft:baked_potato'] },
            { items: `${NS}:vinegar` },
        ],
        result: `${NS}:potato_shreds`,
    },
    {
        id: 'garlic_vegetable', priority: 22, station: 'iron_pot',
        flavors: { vegetable: { min: 3 } },
        mustHave: [
            { items: `${NS}:garlic` },
            { items: [`${NS}:napacabbage`, `${NS}:garlicsprout`, `${NS}:garlicscape`, 'minecraft:carrot', 'minecraft:beetroot'] },
        ],
        result: `${NS}:garlic_vegetable`,
    },
    {
        id: 'cabbage_stirfry', priority: 20, station: 'iron_pot',
        flavors: { vegetable: { min: 3 } },
        mustHave: [{ items: `${NS}:napacabbage` }],
        result: `${NS}:cabbage_stirfry`,
    },
    {
        id: 'stir_fried_greens', priority: 18, station: 'iron_pot',
        flavors: { vegetable: { min: 3 }, oily: { min: 1 } },
        mustHave: [{ items: `${NS}:cookingoil` }],
        result: `${NS}:stir_fried_greens`,
    },

    /* ================= 菌菇 ================= */
    {
        id: 'mushroom_stirfry', priority: 18, station: 'iron_pot',
        flavors: { umami: { min: 3 } },
        mustHave: [{ items: ['minecraft:brown_mushroom', 'minecraft:red_mushroom'] }],
        result: `${NS}:mushroom_stirfry`,
    },

    /* ================= 肉类 ================= */
    {
        id: 'fish_fragrant_pork', priority: 38, station: 'iron_pot',
        flavors: { sour: { min: 1 }, spicy: { min: 1 }, sweet: { min: 1 }, umami: { min: 3 } },
        mustHave: [
            { items: ['minecraft:porkchop', 'minecraft:cooked_porkchop'] },
            { items: `${NS}:pickledcabbage` },
        ],
        result: `${NS}:fish_fragrant_pork`,
    },
    {
        id: 'twice_cooked_pork', priority: 36, station: 'iron_pot',
        flavors: { spicy: { min: 1 }, umami: { min: 3 }, oily: { min: 1 } },
        mustHave: [
            { items: ['minecraft:porkchop', 'minecraft:cooked_porkchop'] },
            { items: `${NS}:doubanjiang` },
        ],
        result: `${NS}:twice_cooked_pork`,
    },
    {
        id: 'braised_pork', priority: 36, station: 'iron_pot',
        flavors: { umami: { min: 3 }, sweet: { min: 1 } },
        mustHave: [
            { items: ['minecraft:porkchop', 'minecraft:cooked_porkchop'] },
            { items: `${NS}:soysauce` },
            { items: 'minecraft:sugar' },
        ],
        result: `${NS}:braised_pork`,
    },
    {
        id: 'beef_potato_stew', priority: 32, station: 'iron_pot',
        flavors: { umami: { min: 4 }, starchy: { min: 2 } },
        mustHave: [
            { items: ['minecraft:beef', 'minecraft:cooked_beef'] },
            { items: ['minecraft:potato', 'minecraft:baked_potato'] },
        ],
        result: `${NS}:beef_potato_stew`,
    },
    {
        id: 'cumin_mutton', priority: 30, station: 'iron_pot',
        flavors: { umami: { min: 3 }, spicy: { min: 1 }, numbing: { min: 1 } },
        mustHave: [
            { items: ['minecraft:mutton', 'minecraft:cooked_mutton'] },
            { items: `${NS}:peppercorn` },
        ],
        result: `${NS}:cumin_mutton`,
    },
    {
        id: 'chili_pork', priority: 28, station: 'iron_pot',
        flavors: { spicy: { min: 2 }, umami: { min: 2 } },
        mustHave: [
            { items: ['minecraft:porkchop', 'minecraft:cooked_porkchop', 'minecraft:beef', 'minecraft:cooked_beef'] },
            { items: [`${NS}:red_chilli`, `${NS}:chilipowder`] },
        ],
        result: `${NS}:chili_pork`,
    },
    {
        id: 'scallion_beef', priority: 28, station: 'iron_pot',
        flavors: { umami: { min: 4 }, vegetable: { min: 1 } },
        mustHave: [
            { items: ['minecraft:beef', 'minecraft:cooked_beef'] },
            { items: `${NS}:bigscallion` },
        ],
        result: `${NS}:scallion_beef`,
    },
    {
        id: 'chicken_mushroom', priority: 28, station: 'iron_pot',
        flavors: { umami: { min: 4 }, vegetable: { min: 1 } },
        mustHave: [
            { items: ['minecraft:chicken', 'minecraft:cooked_chicken'] },
            { items: ['minecraft:brown_mushroom', 'minecraft:red_mushroom'] },
        ],
        result: `${NS}:chicken_mushroom`,
    },
    {
        id: 'ginger_chicken', priority: 26, station: 'iron_pot',
        flavors: { umami: { min: 3 }, spicy: { min: 1 } },
        mustHave: [
            { items: ['minecraft:chicken', 'minecraft:cooked_chicken'] },
            { items: `${NS}:ginger` },
        ],
        result: `${NS}:ginger_chicken`,
    },
    {
        id: 'meat_stirfry', priority: 12, station: 'iron_pot',
        flavors: { umami: { min: 2 } },
        mustHave: [{
            items: [
                'minecraft:beef', 'minecraft:cooked_beef', 'minecraft:porkchop', 'minecraft:cooked_porkchop',
                'minecraft:chicken', 'minecraft:cooked_chicken', 'minecraft:mutton', 'minecraft:cooked_mutton',
            ],
        }],
        result: `${NS}:meat_stirfry`,
    },

    /* ================= 鱼 =================
     * 注意：mustHave 的 items 列表必须是「同质的」——要么全是物品 id，要么单个 "#标签"。
     * 混着写（"#minecraft:fishes" 和具体 id 并列）在加载世界时会报
     * "Failed to parse"（HolderSetCodec 的 ensureHomogenous 校验）。
     * 所以这里统一用自己定义的 #chinesedelight:fish 标签，
     * 该标签在 tags/item/fish.json 里同时收进了生鱼和熟鱼。
     */
    {
        id: 'braised_fish', priority: 34, station: 'iron_pot',
        flavors: { umami: { min: 3 }, salty: { min: 1 } },
        mustHave: [
            { items: `#${NS}:fish` },
            { items: `${NS}:soysauce` },
        ],
        result: `${NS}:braised_fish`,
    },
    {
        id: 'steamed_fish', priority: 32, station: 'iron_pot',
        flavors: { umami: { min: 2 }, vegetable: { min: 1 } },
        mustHave: [
            { items: `#${NS}:fish` },
            { items: `${NS}:ginger` },
        ],
        result: `${NS}:steamed_fish`,
    },
    {
        id: 'fish_dish', priority: 10, station: 'iron_pot',
        flavors: { umami: { min: 1 } },
        mustHave: [{ items: `#${NS}:fish` }],
        result: `${NS}:braised_fish`,
    },

    /* ================= 主食 ================= */
    {
        id: 'yangzhou_fried_rice', priority: 34, station: 'iron_pot',
        flavors: { starchy: { min: 2 }, umami: { min: 2 } },
        mustHave: [
            { items: `${NS}:riceporridge` },
            { items: 'minecraft:egg' },
            { items: ['minecraft:carrot', `${NS}:edamame`, `${NS}:smallscallion`] },
        ],
        result: `${NS}:yangzhou_fried_rice`,
    },
    {
        id: 'beef_noodle_soup', priority: 36, station: 'iron_pot',
        flavors: { starchy: { min: 3 }, umami: { min: 4 } },
        mustHave: [
            { items: `${NS}:noodles` },
            { items: ['minecraft:beef', 'minecraft:cooked_beef'] },
        ],
        result: `${NS}:beef_noodle_soup`,
    },
    {
        id: 'dumpling', priority: 30, station: 'iron_pot',
        flavors: { starchy: { min: 3 }, umami: { min: 3 } },
        mustHave: [
            { items: [`${NS}:dough`, `${NS}:flour`] },
            { items: ['minecraft:porkchop', 'minecraft:cooked_porkchop', `${NS}:napacabbage`, `${NS}:garlicsprout`] },
        ],
        result: `${NS}:dumpling`,
    },
    {
        id: 'egg_fried_rice', priority: 30, station: 'iron_pot',
        flavors: { starchy: { min: 3 }, umami: { min: 2 }, oily: { min: 1 } },
        mustHave: [{ items: `${NS}:riceporridge` }, { items: 'minecraft:egg' }],
        result: `${NS}:egg_fried_rice`,
    },
    {
        id: 'fried_noodles', priority: 20, station: 'iron_pot',
        flavors: { starchy: { min: 3 }, oily: { min: 1 }, vegetable: { min: 1 } },
        mustHave: [{ items: `${NS}:noodles` }],
        result: `${NS}:fried_noodles`,
    },
    {
        id: 'sour_noodles', priority: 32, station: 'iron_pot',
        flavors: { starchy: { min: 3 }, sour: { min: 2 }, spicy: { min: 1 } },
        mustHave: [{ items: `${NS}:noodles` }, { items: `${NS}:vinegar` }],
        result: `${NS}:sour_noodles`,
    },
    {
        id: 'scallion_oil_noodles', priority: 34, station: 'iron_pot',
        flavors: { starchy: { min: 3 }, oily: { min: 2 } },
        mustHave: [{ items: `${NS}:noodles` }, { items: `${NS}:bigscallion` }],
        result: `${NS}:scallion_oil_noodles`,
    },
    {
        id: 'congee', priority: 20, station: 'iron_pot',
        flavors: { starchy: { min: 4 }, salty: { max: 2 } },
        result: `${NS}:riceporridge`,
    },
    {
        id: 'steamed_bun_dish', priority: 14, station: 'iron_pot',
        flavors: { starchy: { min: 3 } },
        mustHave: [{ items: `${NS}:steamedbun` }],
        result: `${NS}:steamedbun`,
    },
    {
        id: 'noodle_soup', priority: 12, station: 'iron_pot',
        flavors: { starchy: { min: 2 } },
        mustHave: [{ items: `${NS}:noodles` }],
        result: `${NS}:fried_noodles`,
    },

    /* ================= 小吃 ================= */
    {
        id: 'scallion_pancake', priority: 26, station: 'iron_pot',
        flavors: { starchy: { min: 2 }, oily: { min: 2 } },
        mustHave: [
            { items: [`${NS}:dough`, `${NS}:flour`] },
            { items: `${NS}:smallscallion` },
        ],
        result: `${NS}:scallion_pancake`,
    },
    {
        id: 'fried_eggs', priority: 16, station: 'iron_pot',
        flavors: { umami: { min: 2 }, oily: { min: 2 } },
        result: `${NS}:omelet`,
    },

    /* ================= 汤 ================= */
    {
        id: 'hot_sour_soup', priority: 36, station: 'iron_pot',
        flavors: { sour: { min: 2 }, spicy: { min: 1 }, umami: { min: 2 } },
        mustHave: [{ items: `${NS}:vinegar` }, { items: [`${NS}:red_chilli`, `${NS}:chilipowder`] }],
        result: `${NS}:hot_sour_soup`,
    },
    {
        id: 'egg_drop_soup', priority: 24, station: 'iron_pot',
        flavors: { umami: { min: 2 }, salty: { min: 1 } },
        mustHave: [{ items: 'minecraft:egg' }, { items: `${NS}:cookingsalt` }],
        result: `${NS}:egg_drop_soup`,
    },
    {
        id: 'kelp_soup', priority: 20, station: 'iron_pot',
        flavors: { salty: { min: 2 }, umami: { min: 2 } },
        mustHave: [{ items: ['minecraft:kelp', 'minecraft:dried_kelp'] }],
        result: `${NS}:kelp_soup`,
    },
    {
        id: 'mushroom_soup', priority: 18, station: 'iron_pot',
        flavors: { umami: { min: 2 }, vegetable: { min: 1 } },
        mustHave: [{ items: ['minecraft:brown_mushroom', 'minecraft:red_mushroom'] }],
        result: `${NS}:mushroom_soup`,
    },

    /* ================= 凉菜 ================= */
    {
        id: 'cold_kelp', priority: 22, station: 'iron_pot',
        flavors: { salty: { min: 2 }, sour: { min: 1 } },
        mustHave: [{ items: ['minecraft:kelp', 'minecraft:dried_kelp'] }, { items: `${NS}:vinegar` }],
        result: `${NS}:cold_kelp`,
    },
    {
        id: 'pickled_cabbage_dish', priority: 18, station: 'iron_pot',
        flavors: { sour: { min: 2 } },
        mustHave: [{ items: `${NS}:pickledcabbage` }],
        result: `${NS}:cold_kelp`,
    },

    /* ================= 兜底（条件刻意写得松） =================
     * 分层设计：越靠后的兜底条件越松，最后一层「随便炒炒」不带任何味觉要求，
     * 于是任何食材组合都至少能落到一道菜上，锅基本不会再出惩罚料理。
     */
    {
        id: 'vegetable_stew', priority: 8, station: 'iron_pot', fallback: true,
        flavors: { vegetable: { min: 2 } },
        result: `${NS}:vegetable_stew`,
    },
    {
        id: 'meat_stirfry_fallback', priority: 7, station: 'iron_pot', fallback: true,
        flavors: { umami: { min: 2 } },
        result: `${NS}:meat_stirfry`,
    },
    {
        id: 'mixed_stirfry', priority: 6, station: 'iron_pot', fallback: true,
        flavors: { vegetable: { min: 1 } },
        result: `${NS}:stir_fried_greens`,
    },
    {
        id: 'fish_stirfry_fallback', priority: 5, station: 'iron_pot', fallback: true,
        flavors: { umami: { min: 1 } },
        result: `${NS}:braised_fish`,
    },
    {
        id: 'oil_salt_mix', priority: 4, station: 'iron_pot', fallback: true,
        flavors: { oily: { min: 1 } },
        result: `${NS}:stir_fried_greens`,
    },
    {
        id: 'plain_fried', priority: 3, station: 'iron_pot', fallback: true,
        result: `${NS}:stir_fried_greens`,
    },

    /* ---- 惩罚料理（做不出任何菜时按食材类别出，吃了中毒 + 恶心） ---- */
    { id: 'mush_meat', failure: true, failureCategory: 'meat', station: 'iron_pot', result: `${NS}:mush_meat` },
    { id: 'mush_fish', failure: true, failureCategory: 'fish', station: 'iron_pot', result: `${NS}:mush_fish` },
    { id: 'mush_veggie', failure: true, failureCategory: 'veggie', station: 'iron_pot', result: `${NS}:mush_veggie` },
    { id: 'mush_default', failure: true, failureCategory: 'default', station: 'iron_pot', result: `${NS}:mush_default` },
];

/**
 * 自定义物品标签。用在 must_have 里可以一次覆盖「生鱼 + 熟鱼」。
 *
 * 这里可以混写标签和具体物品（标签文件的 values 允许多种写法），
 * 但 must_have 的 items 列表不行——所以复杂条件都收敛到标签里。
 */
const TAGS = [
    {
        id: 'fish',
        // values 写进数据包标签文件（可以混写标签和物品）
        values: ['#minecraft:fishes', 'minecraft:cooked_cod', 'minecraft:cooked_salmon'],
        // members 只是给校验器用的「这个标签里有哪些物品」，
        // 因为标签内容在生成期没法真实解析。改 values 时记得同步这里。
        members: [
            'minecraft:cod', 'minecraft:salmon', 'minecraft:tropical_fish', 'minecraft:pufferfish',
            'minecraft:cooked_cod', 'minecraft:cooked_salmon',
        ],
    },
];

/** 石磨：output_mode 是 bucket 的话要用空桶取。 */
const GRINDING = [
    { id: 'soymilk', input: `${NS}:soybean`, water: true, time: 100, outputMode: 'bucket', result: { id: `${NS}:soymilk_bucket`, count: 1 } },
    { id: 'flour', input: 'minecraft:wheat', water: false, time: 80, outputMode: 'item', result: { id: `${NS}:flour`, count: 1 } },
    { id: 'starch', input: 'minecraft:potato', water: false, time: 100, outputMode: 'item', result: { id: `${NS}:starch`, count: 1 } },
    { id: 'chili_powder', input: `${NS}:red_chilli`, water: false, time: 80, outputMode: 'item', result: { id: `${NS}:chilipowder`, count: 1 } },
];

/** 陶缸：内容物必须和 ingredients 完全一致（数量也对上）。 */
const FERMENTING = [
    {
        id: 'doubanjiang', water: true, time: 2400,
        ingredients: [
            { items: `${NS}:soybean`, min: 2 },
            { items: `${NS}:red_chilli`, min: 1 },
            { items: `${NS}:cookingsalt`, min: 1 },
        ],
        result: { id: `${NS}:doubanjiang`, count: 2 },
    },
    {
        id: 'pickled_cabbage', water: true, time: 3600,
        ingredients: [
            { items: `${NS}:napacabbage`, min: 1 },
            { items: `${NS}:cookingsalt`, min: 1 },
        ],
        result: { id: `${NS}:pickledcabbage`, count: 2 },
    },
    {
        id: 'yogurt', water: false, time: 600,
        ingredients: [{ items: 'minecraft:milk_bucket', min: 1 }],
        result: { id: `${NS}:yogurt_bucket`, count: 1 },
    },
    {
        id: 'dough', water: true, time: 200,
        ingredients: [{ items: `${NS}:flour`, min: 2 }],
        result: { id: `${NS}:dough`, count: 2 },
    },
    {
        id: 'dried_tofu', water: false, time: 1200,
        ingredients: [
            { items: `${NS}:tofu`, min: 2 },
            { items: `${NS}:cookingsalt`, min: 1 },
        ],
        result: { id: `${NS}:driedtofu`, count: 2 },
    },
];

/**
 * 菜品成品物品。
 *
 * <p>{@code existing: true} 表示这个物品已经在 ModItems 里注册过了（早期就有的鸡蛋饼、白粥、馒头），
 * Java 侧不能重复注册，否则启动时会报 "Duplicate registration"。这类条目仍然保留，
 * 因为它们承载语言词条与食用属性说明。
 *
 * <p>zh/en 用来补语言条目；nutrition/saturation/effect 决定吃了有什么好处。
 * effect 取值：speed 迅捷 / haste 急迫 / regen 生命恢复 / strength 力量 / resist 抗性提升 / night_vision 夜视；
 *              penalty 表示惩罚料理（中毒 + 恶心）。
 */
const DISH_ITEMS = [
    // 蛋
    { id: 'omelet', zh: '鸡蛋饼', en: 'Omelet', nutrition: 6, saturation: 0.6, existing: true },
    { id: 'scallion_egg', zh: '大葱炒蛋', en: 'Scallion Scrambled Eggs', nutrition: 7, saturation: 0.7 },
    { id: 'steamed_egg', zh: '蒸蛋', en: 'Steamed Egg', nutrition: 5, saturation: 0.7, effect: 'regen' },
    // 豆腐 / 豆制品
    { id: 'mapo_tofu', zh: '麻婆豆腐', en: 'Mapo Tofu', nutrition: 8, saturation: 1.0, effect: 'speed' },
    { id: 'spicy_tofu', zh: '辣豆腐', en: 'Spicy Tofu', nutrition: 7, saturation: 0.8, effect: 'speed' },
    { id: 'home_style_tofu', zh: '家常豆腐', en: 'Home-style Tofu', nutrition: 7, saturation: 0.8 },
    { id: 'mushroom_tofu', zh: '香菇豆腐', en: 'Mushroom Tofu', nutrition: 7, saturation: 0.8 },
    { id: 'cabbage_tofu_soup', zh: '白菜豆腐汤', en: 'Cabbage Tofu Soup', nutrition: 6, saturation: 0.7, effect: 'regen' },
    { id: 'edamame_dish', zh: '煮毛豆', en: 'Boiled Edamame', nutrition: 4, saturation: 0.5 },
    { id: 'dried_tofu_stirfry', zh: '炒豆干', en: 'Stir-fried Dried Tofu', nutrition: 7, saturation: 0.8, effect: 'strength' },
    // 蔬菜
    { id: 'garlic_vegetable', zh: '蒜蓉青菜', en: 'Garlic Greens', nutrition: 6, saturation: 0.6 },
    { id: 'stir_fried_greens', zh: '炒时蔬', en: 'Stir-fried Greens', nutrition: 5, saturation: 0.5 },
    { id: 'cabbage_stirfry', zh: '手撕包菜', en: 'Hand-torn Cabbage', nutrition: 5, saturation: 0.5 },
    { id: 'potato_shreds', zh: '炒土豆丝', en: 'Stir-fried Potato Shreds', nutrition: 6, saturation: 0.6 },
    { id: 'sour_potato_shreds', zh: '酸辣土豆丝', en: 'Hot & Sour Potato Shreds', nutrition: 7, saturation: 0.7, effect: 'haste' },
    { id: 'vegetable_stew', zh: '蔬菜杂烩', en: 'Vegetable Stew', nutrition: 6, saturation: 0.6 },
    { id: 'mushroom_stirfry', zh: '炒蘑菇', en: 'Stir-fried Mushrooms', nutrition: 6, saturation: 0.6 },
    // 肉
    { id: 'fish_fragrant_pork', zh: '鱼香肉丝', en: 'Fish-fragrant Pork', nutrition: 9, saturation: 1.1, effect: 'speed' },
    { id: 'twice_cooked_pork', zh: '回锅肉', en: 'Twice-cooked Pork', nutrition: 9, saturation: 1.1, effect: 'strength' },
    { id: 'braised_pork', zh: '红烧肉', en: 'Braised Pork', nutrition: 10, saturation: 1.2, effect: 'resist' },
    { id: 'beef_potato_stew', zh: '土豆炖牛肉', en: 'Beef & Potato Stew', nutrition: 10, saturation: 1.2, effect: 'strength' },
    { id: 'chili_pork', zh: '辣椒炒肉', en: 'Chili Pork', nutrition: 8, saturation: 1.0, effect: 'speed' },
    { id: 'scallion_beef', zh: '葱爆牛肉', en: 'Scallion Beef', nutrition: 9, saturation: 1.1, effect: 'strength' },
    { id: 'cumin_mutton', zh: '孜然羊肉', en: 'Cumin Lamb', nutrition: 9, saturation: 1.1, effect: 'strength' },
    { id: 'chicken_mushroom', zh: '小鸡炖蘑菇', en: 'Chicken & Mushroom', nutrition: 9, saturation: 1.1, effect: 'regen' },
    { id: 'ginger_chicken', zh: '姜葱鸡', en: 'Ginger Chicken', nutrition: 8, saturation: 1.0, effect: 'regen' },
    { id: 'meat_stirfry', zh: '炒肉', en: 'Stir-fried Meat', nutrition: 7, saturation: 0.8 },
    // 鱼
    { id: 'steamed_fish', zh: '清蒸鱼', en: 'Steamed Fish', nutrition: 8, saturation: 1.0, effect: 'night_vision' },
    { id: 'braised_fish', zh: '红烧鱼', en: 'Braised Fish', nutrition: 8, saturation: 1.0, effect: 'night_vision' },
    // 主食
    { id: 'yangzhou_fried_rice', zh: '扬州炒饭', en: 'Yangzhou Fried Rice', nutrition: 10, saturation: 1.2, effect: 'haste' },
    { id: 'egg_fried_rice', zh: '蛋炒饭', en: 'Egg Fried Rice', nutrition: 9, saturation: 1.0, effect: 'haste' },
    { id: 'beef_noodle_soup', zh: '牛肉面', en: 'Beef Noodle Soup', nutrition: 10, saturation: 1.2, effect: 'strength' },
    { id: 'scallion_oil_noodles', zh: '葱油拌面', en: 'Scallion Oil Noodles', nutrition: 8, saturation: 0.9, effect: 'haste' },
    { id: 'fried_noodles', zh: '炒面', en: 'Fried Noodles', nutrition: 8, saturation: 0.9 },
    { id: 'sour_noodles', zh: '酸辣面', en: 'Hot & Sour Noodles', nutrition: 8, saturation: 0.9, effect: 'speed' },
    { id: 'riceporridge', zh: '白粥', en: 'Rice Porridge', nutrition: 5, saturation: 0.6, effect: 'regen', existing: true },
    { id: 'steamedbun', zh: '馒头', en: 'Steamed Bun', nutrition: 6, saturation: 0.7, existing: true },
    { id: 'dumpling', zh: '饺子', en: 'Dumplings', nutrition: 9, saturation: 1.1, effect: 'resist' },
    // 小吃
    { id: 'scallion_pancake', zh: '葱油饼', en: 'Scallion Pancake', nutrition: 7, saturation: 0.8 },
    // 汤
    { id: 'egg_drop_soup', zh: '蛋花汤', en: 'Egg Drop Soup', nutrition: 5, saturation: 0.7, effect: 'regen' },
    { id: 'hot_sour_soup', zh: '酸辣汤', en: 'Hot & Sour Soup', nutrition: 6, saturation: 0.8, effect: 'speed' },
    { id: 'kelp_soup', zh: '海带汤', en: 'Kelp Soup', nutrition: 5, saturation: 0.7, effect: 'night_vision' },
    { id: 'mushroom_soup', zh: '蘑菇汤', en: 'Mushroom Soup', nutrition: 6, saturation: 0.8, effect: 'regen' },
    // 凉菜
    { id: 'cold_kelp', zh: '凉拌菜', en: 'Cold Salad', nutrition: 4, saturation: 0.5 },
    // 惩罚料理
    { id: 'mush_default', zh: '糊糊', en: 'Goop', nutrition: 2, saturation: 0.1, effect: 'penalty' },
    { id: 'mush_meat', zh: '夹生肉糊', en: 'Undercooked Meat Mash', nutrition: 2, saturation: 0.1, effect: 'penalty' },
    { id: 'mush_fish', zh: '腥味糊', en: 'Fishy Mash', nutrition: 2, saturation: 0.1, effect: 'penalty' },
    { id: 'mush_veggie', zh: '烂菜糊', en: 'Mushy Greens', nutrition: 2, saturation: 0.1, effect: 'penalty' },
];

/**
 * 需要生成物品模型、但已经由 ModItems 注册的物品
 * （不属于 DISH_ITEMS，所以不会被自动处理；少了模型游戏里会显示紫黑格）。
 */
const EXTRA_ITEM_MODELS = [
    'red_chilli',
    // 姜既是种子也是食材，物品模型必须用 item/generated 指向 ginger.png；
    // 之前它错指向 block/gingercrop，导致物品显示成作物贴图/破面。
    'ginger',
];

module.exports = {
    NS, FLAVORS, FLAVOR_NAMES, INGREDIENTS, DISHES, TAGS, GRINDING, FERMENTING,
    DISH_ITEMS, EXTRA_ITEM_MODELS,
};
