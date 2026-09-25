package com.qiyi.chinesedelight.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 一道菜。注册表 id 形如 {@code chinesedelight:mapo_tofu}，文件放在
 * {@code data/<namespace>/chinesedelight/dish/<name>.json}。
 *
 * <p>判定顺序：先过滤硬性条件（厨具、味觉区间、必须有、不能有），再取 {@link #priority()} 最大的；
 * {@link #fallback()} 的菜只有在没有任何普通菜匹配时才考虑；{@link #failure()} 的菜是完全没人匹配时的惩罚料理。</p>
 */
public record Dish(
        int priority,
        Optional<String> station,
        Map<FlavorType, FlavorRange> flavors,
        List<ItemRequirement> mustHave,
        List<HolderSet<Item>> mustNotHave,
        ItemStack result,
        boolean fallback,
        boolean failure,
        Optional<String> failureCategory
) {
    public static final Codec<Dish> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("priority", 0).forGetter(Dish::priority),
            Codec.STRING.optionalFieldOf("station").forGetter(Dish::station),
            Codec.unboundedMap(FlavorType.CODEC, FlavorRange.CODEC)
                    .optionalFieldOf("flavors", Map.of()).forGetter(Dish::flavors),
            ItemRequirement.CODEC.listOf().optionalFieldOf("must_have", List.of()).forGetter(Dish::mustHave),
            RegistryCodecs.homogeneousList(Registries.ITEM).listOf()
                    .optionalFieldOf("must_not_have", List.of()).forGetter(Dish::mustNotHave),
            ItemStack.CODEC.fieldOf("result").forGetter(Dish::result),
            Codec.BOOL.optionalFieldOf("fallback", false).forGetter(Dish::fallback),
            Codec.BOOL.optionalFieldOf("failure", false).forGetter(Dish::failure),
            Codec.STRING.optionalFieldOf("failure_category").forGetter(Dish::failureCategory)
    ).apply(instance, Dish::new));
}
