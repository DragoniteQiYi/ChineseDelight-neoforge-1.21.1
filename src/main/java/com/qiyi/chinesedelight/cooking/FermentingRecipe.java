package com.qiyi.chinesedelight.cooking;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * 陶缸配方（发酵 / 腌制 / 和面）：内容物必须与 {@code ingredients} 完全一致（数量也要对上）。
 * 注册表 {@code chinesedelight:fermenting}，文件放在 {@code data/<namespace>/chinesedelight/fermenting/<name>.json}。
 */
public record FermentingRecipe(List<ItemRequirement> ingredients, boolean water, int time, ItemStack result) {
    public static final Codec<FermentingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemRequirement.CODEC.listOf().fieldOf("ingredients").forGetter(FermentingRecipe::ingredients),
            Codec.BOOL.optionalFieldOf("water", false).forGetter(FermentingRecipe::water),
            Codec.INT.optionalFieldOf("time", 1200).forGetter(FermentingRecipe::time),
            ItemStack.CODEC.fieldOf("result").forGetter(FermentingRecipe::result)
    ).apply(instance, FermentingRecipe::new));

    public int requiredItems() {
        return this.ingredients.stream().mapToInt(ItemRequirement::min).sum();
    }

    /** 严格匹配：每一项都满足最少数量，且没有多余的东西。 */
    public boolean matches(List<ItemStack> contents) {
        List<ItemStack> pool = new ArrayList<>();
        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                pool.add(stack.copy());
            }
        }
        if (pool.stream().mapToInt(ItemStack::getCount).sum() != this.requiredItems()) {
            return false;
        }
        for (ItemRequirement requirement : this.ingredients) {
            int needed = requirement.min();
            for (ItemStack stack : pool) {
                if (needed <= 0) {
                    break;
                }
                if (requirement.matches(stack)) {
                    int taken = Math.min(needed, stack.getCount());
                    stack.shrink(taken);
                    needed -= taken;
                }
            }
            if (needed > 0) {
                return false;
            }
        }
        return pool.stream().allMatch(ItemStack::isEmpty);
    }
}
