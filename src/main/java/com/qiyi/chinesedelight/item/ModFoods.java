package com.qiyi.chinesedelight.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Items;

/**
 * 中华乐事 (Chinese Delight) 的食物属性。
 *
 * <p>1.21.1 还没有 {@code Consumable} 组件体系（那是 1.21.2 引入的），所以
 * 「喝一口会怎样」的副作用直接挂在 {@code FoodProperties.Builder#effect} 上，
 * 「喝完剩下什么容器」用 {@code FoodProperties.Builder#usingConvertsTo(ItemLike)} 表达。
 * 1.21.5 里的 {@code Item.Properties#usingConvertsTo} 在这里并不存在。
 *
 * <p>注意：这里刻意不用 {@code @link}，因为 1.21.1 的 {@code FoodProperties} 是 record，
 * 里面同时有一个<b>私有</b> {@code usingConvertsTo} 字段和一个<b>公有</b>
 * {@code FoodProperties.Builder#usingConvertsTo} 方法。{@code @link} 会解析到那个私有字段上，
 * IDE 于是报“无法访问符号”，但代码本身是能正常编译的。
 */
public class ModFoods {
    /** 生豆浆：4 点饱食度，喝完恶心 + 中毒各 5 秒。 */
    public static final FoodProperties RAW_SOYMILK = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.3F)
            .effect(() -> new MobEffectInstance(MobEffects.CONFUSION, 100, 0), 1.0F)
            .effect(() -> new MobEffectInstance(MobEffects.POISON, 100, 0), 1.0F)
            .usingConvertsTo(Items.BUCKET)
            .build();

    /** 熟豆浆：6 点饱食度，没有副作用。 */
    public static final FoodProperties COOKED_SOYMILK = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.6F)
            .usingConvertsTo(Items.BUCKET)
            .build();

    /** 酸奶：5 点饱食度，喝下去有点回饱。 */
    public static final FoodProperties YOGURT = new FoodProperties.Builder()
            .nutrition(5)
            .saturationModifier(0.5F)
            .usingConvertsTo(Items.BUCKET)
            .build();

    public static final FoodProperties TOFU = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.3F)
            .build();

    public static final FoodProperties DRIED_TOFU = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.5F)
            .build();

    public static final FoodProperties PICKLED_CABBAGE = new FoodProperties.Builder()
            .nutrition(3)
            .saturationModifier(0.4F)
            .build();

    public static final FoodProperties NOODLES = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.6F)
            .build();

    public static final FoodProperties STEAMED_BUN = new FoodProperties.Builder()
            .nutrition(6)
            .saturationModifier(0.7F)
            .build();
}
