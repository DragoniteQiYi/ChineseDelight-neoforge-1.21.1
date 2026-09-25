package com.qiyi.chinesedelight.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜品的“食用属性”表：出菜规则在数据包（{@code chinesedelight:dish}）里，吃了有什么效果在这里定。
 *
 * <p>1.21.1 用 {@code FoodProperties.Builder#effect} 直接表达食用效果；1.21.5 的
 * {@code Consumable} 组件在这个版本还不存在。
 *
 * <p>这里用 {@code @code} 而不是 {@code @link}：{@code FoodProperties} 是 record，
 * 私有字段与 Builder 方法同名，{@code @link} 会解析到私有字段上让 IDE 报错。
 */
public final class ModDishItems {
    public record DishFood(String id, int nutrition, float saturation, List<MobEffectInstance> effects) {
    }

    /** 吃了会中毒 + 恶心的惩罚料理。 */
    private static List<MobEffectInstance> penalty() {
        return List.of(
                new MobEffectInstance(MobEffects.POISON, 100, 0),
                new MobEffectInstance(MobEffects.CONFUSION, 200, 0));
    }

    public static final List<DishFood> ALL = List.of(
            new DishFood("mapo_tofu", 8, 1.0F, List.of(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 300, 0))),
            new DishFood("curry", 10, 1.2F, List.of(new MobEffectInstance(MobEffects.DIG_SPEED, 300, 0))),
            new DishFood("vegetable_stew", 6, 0.6F, List.of()),
            new DishFood("mush_default", 2, 0.1F, penalty()),
            new DishFood("mush_meat", 2, 0.1F, penalty()),
            new DishFood("mush_fish", 2, 0.1F, penalty()),
            new DishFood("mush_veggie", 2, 0.1F, penalty()));

    private ModDishItems() {
    }

    private static final Map<String, DeferredItem<Item>> REGISTERED = new LinkedHashMap<>();

    /** 按 id 取菜品物品（数据包里的 result 用的就是同一个 id）。 */
    public static DeferredItem<Item> get(String id) {
        DeferredItem<Item> item = REGISTERED.get(id);
        if (item == null) {
            throw new IllegalArgumentException("Unknown dish item: " + id);
        }
        return item;
    }

    public static void register(DeferredRegister.Items items) {
        for (DishFood food : ALL) {
            FoodProperties.Builder builder = new FoodProperties.Builder()
                    .nutrition(food.nutrition())
                    .saturationModifier(food.saturation());
            for (MobEffectInstance effect : food.effects()) {
                builder.effect(() -> effect, 1.0F);
            }
            FoodProperties properties = builder.build();
            REGISTERED.put(food.id(), items.registerItem(food.id(), Item::new, new Item.Properties().food(properties)));
        }
    }
}
