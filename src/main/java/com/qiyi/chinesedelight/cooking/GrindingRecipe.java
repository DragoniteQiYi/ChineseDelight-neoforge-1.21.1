package com.qiyi.chinesedelight.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 石磨配方：把某种食材磨成产物。注册表 {@code chinesedelight:grinding}，
 * 文件放在 {@code data/<namespace>/chinesedelight/grinding/<name>.json}。
 *
 * <p>{@code output_mode} 为 {@code bucket} 时（例如豆浆），石磨只累积“份数”，
 * 玩家要拿空桶右键才能取走；为 {@code item} 时（例如面粉），产物直接堆在石磨里，空手右键拿走。</p>
 */
public record GrindingRecipe(HolderSet<Item> input, boolean water, int time, String outputMode, ItemStack result) {
    public static final String MODE_ITEM = "item";
    public static final String MODE_BUCKET = "bucket";

    public static final Codec<GrindingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            RegistryCodecs.homogeneousList(Registries.ITEM).fieldOf("input").forGetter(GrindingRecipe::input),
            Codec.BOOL.optionalFieldOf("water", false).forGetter(GrindingRecipe::water),
            Codec.INT.optionalFieldOf("time", 100).forGetter(GrindingRecipe::time),
            Codec.STRING.optionalFieldOf("output_mode", MODE_ITEM).forGetter(GrindingRecipe::outputMode),
            ItemStack.CODEC.fieldOf("result").forGetter(GrindingRecipe::result)
    ).apply(instance, GrindingRecipe::new));

    public boolean usesBucket() {
        return MODE_BUCKET.equals(this.outputMode);
    }

    public boolean matches(ItemStack stack) {
        return !stack.isEmpty() && this.input.contains(stack.getItemHolder());
    }
}
