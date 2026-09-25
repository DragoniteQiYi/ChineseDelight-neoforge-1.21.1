package com.qiyi.chinesedelight.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * “必须包含某样东西（至少 N 个）”。{@code items} 可以写物品 id、物品 id 列表，或者 {@code "#标签"}。
 */
public record ItemRequirement(HolderSet<Item> items, int min) {
    public static final Codec<ItemRequirement> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(ItemRequirement::items),
            Codec.INT.optionalFieldOf("min", 1).forGetter(ItemRequirement::min)
    ).apply(instance, ItemRequirement::new));

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && this.items.contains(stack.getItemHolder());
    }
}
