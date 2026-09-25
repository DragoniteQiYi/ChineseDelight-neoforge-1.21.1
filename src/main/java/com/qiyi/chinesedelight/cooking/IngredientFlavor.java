package com.qiyi.chinesedelight.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import java.util.Optional;

/**
 * 一种食材（或一整类食材）的味道数据。
 * 注册表 id 形如 {@code chinesedelight:carrot}，文件放在 {@code data/<namespace>/chinesedelight/ingredient/<name>.json}。
 */
public record IngredientFlavor(HolderSet<Item> items, FlavorSet flavors, Optional<String> failureCategory) {
    public static final Codec<IngredientFlavor> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("items").forGetter(IngredientFlavor::items),
            FlavorSet.CODEC.optionalFieldOf("flavors", FlavorSet.EMPTY).forGetter(IngredientFlavor::flavors),
            Codec.STRING.optionalFieldOf("failure_category").forGetter(IngredientFlavor::failureCategory)
    ).apply(instance, IngredientFlavor::new));

    public boolean isTagBased() {
        return this.items.unwrap().left().isPresent();
    }
}
