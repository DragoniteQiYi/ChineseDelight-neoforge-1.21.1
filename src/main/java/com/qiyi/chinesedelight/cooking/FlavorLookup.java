package com.qiyi.chinesedelight.cooking;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 把 {@code chinesedelight:ingredient} 注册表摊平成“物品 → 味道”的查表。
 * 标签条目先铺一层，指定物品的条目最后覆盖（所以单个物品的数值可以盖掉它的通用标签）。
 */
public final class FlavorLookup {
    public static final FlavorLookup EMPTY = new FlavorLookup(Map.of());

    private final Map<Item, IngredientFlavor> byItem;

    private FlavorLookup(Map<Item, IngredientFlavor> byItem) {
        this.byItem = byItem;
    }

    public static FlavorLookup build(Registry<IngredientFlavor> registry) {
        Map<Item, IngredientFlavor> map = new HashMap<>();
        for (IngredientFlavor value : registry) {
            if (value.isTagBased()) {
                put(map, value);
            }
        }
        for (IngredientFlavor value : registry) {
            if (!value.isTagBased()) {
                put(map, value);
            }
        }
        return new FlavorLookup(Map.copyOf(map));
    }

    private static void put(Map<Item, IngredientFlavor> map, IngredientFlavor value) {
        for (Holder<Item> holder : value.items()) {
            map.put(holder.value(), value);
        }
    }

    public IngredientFlavor get(ItemStack stack) {
        return stack.isEmpty() ? null : this.byItem.get(stack.getItem());
    }

    public FlavorSet flavorsOf(ItemStack stack) {
        IngredientFlavor flavor = this.get(stack);
        return flavor == null ? FlavorSet.EMPTY : flavor.flavors();
    }

    /** 没有登记过数值、但能吃的东西（例如别的模组的食物）也算可以下锅。 */
    public boolean isCookable(ItemStack stack) {
        return this.get(stack) != null;
    }

    public boolean isEmpty() {
        return this.byItem.isEmpty();
    }
}
