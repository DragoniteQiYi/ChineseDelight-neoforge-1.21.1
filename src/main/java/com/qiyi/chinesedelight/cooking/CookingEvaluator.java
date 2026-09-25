package com.qiyi.chinesedelight.cooking;

import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 菜品判定器（服务端与客户端都用同一套逻辑：服务端决定出什么菜，客户端用它预测 HUD）。
 */
public final class CookingEvaluator {
    /** 没有登记类别的食材都算这个类别。 */
    public static final String DEFAULT_FAILURE_CATEGORY = "default";

    private CookingEvaluator() {
    }

    /** 一次判定的结果。 */
    public record Result(ResourceLocation id, Dish dish, FlavorSet flavors, boolean failure) {
    }

    /** 每格食材只算一次，把味道加起来。 */
    public static FlavorSet accumulate(Iterable<ItemStack> inputs, FlavorLookup lookup) {
        FlavorSet total = FlavorSet.EMPTY;
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                total = total.plus(lookup.flavorsOf(stack));
            }
        }
        return total;
    }

    /** @return 命中的菜品；锅里没有东西时返回 null */
    @Nullable
    public static Result evaluate(Registry<Dish> dishes, FlavorLookup lookup, List<ItemStack> inputs, String station) {
        List<ItemStack> present = new ArrayList<>();
        for (ItemStack stack : inputs) {
            if (!stack.isEmpty()) {
                present.add(stack);
            }
        }
        if (present.isEmpty()) {
            return null;
        }

        FlavorSet flavors = accumulate(present, lookup);
        Dish best = null;
        ResourceKey<Dish> bestKey = null;
        Dish bestFallback = null;
        ResourceKey<Dish> bestFallbackKey = null;

        for (Map.Entry<ResourceKey<Dish>, Dish> entry : dishes.entrySet()) {
            Dish dish = entry.getValue();
            if (dish.failure() || !stationMatches(dish, station) || !matches(dish, flavors, present)) {
                continue;
            }
            if (dish.fallback()) {
                if (isBetter(bestFallback, bestFallbackKey, dish, entry.getKey())) {
                    bestFallback = dish;
                    bestFallbackKey = entry.getKey();
                }
            } else if (isBetter(best, bestKey, dish, entry.getKey())) {
                best = dish;
                bestKey = entry.getKey();
            }
        }

        if (best != null) {
            return new Result(bestKey.location(), best, flavors, false);
        }
        if (bestFallback != null) {
            return new Result(bestFallbackKey.location(), bestFallback, flavors, false);
        }

        // 什么都做不出来 → 按食材类别给对应的惩罚料理
        String category = dominantFailureCategory(present, lookup);
        for (String wanted : List.of(category, DEFAULT_FAILURE_CATEGORY)) {
            for (Map.Entry<ResourceKey<Dish>, Dish> entry : dishes.entrySet()) {
                Dish dish = entry.getValue();
                if (dish.failure() && dish.failureCategory().orElse(DEFAULT_FAILURE_CATEGORY).equals(wanted)) {
                    return new Result(entry.getKey().location(), dish, flavors, true);
                }
            }
        }
        return null;
    }

    private static boolean stationMatches(Dish dish, String station) {
        return dish.station().isEmpty() || dish.station().get().equals(station);
    }

    private static boolean matches(Dish dish, FlavorSet flavors, List<ItemStack> items) {
        for (Map.Entry<FlavorType, FlavorRange> requirement : dish.flavors().entrySet()) {
            if (!requirement.getValue().test(flavors.get(requirement.getKey()))) {
                return false;
            }
        }
        for (ItemRequirement requirement : dish.mustHave()) {
            int found = 0;
            for (ItemStack stack : items) {
                if (requirement.matches(stack)) {
                    found += stack.getCount();
                }
            }
            if (found < requirement.min()) {
                return false;
            }
        }
        for (HolderSet<Item> banned : dish.mustNotHave()) {
            for (ItemStack stack : items) {
                if (banned.contains(stack.getItemHolder())) {
                    return false;
                }
            }
        }
        return true;
    }

    /** 优先级大的赢；一样大就按注册 id 排序，保证结果稳定。 */
    private static boolean isBetter(@Nullable Dish current, @Nullable ResourceKey<Dish> currentKey, Dish candidate, ResourceKey<Dish> candidateKey) {
        if (current == null || currentKey == null) {
            return true;
        }
        if (candidate.priority() != current.priority()) {
            return candidate.priority() > current.priority();
        }
        return candidateKey.location().compareTo(currentKey.location()) < 0;
    }

    /** 食材里出现最多的失败类别。 */
    public static String dominantFailureCategory(List<ItemStack> items, FlavorLookup lookup) {
        Map<String, Integer> counts = new HashMap<>();
        for (ItemStack stack : items) {
            IngredientFlavor flavor = lookup.get(stack);
            String category = flavor == null ? null : flavor.failureCategory().orElse(null);
            if (category != null) {
                counts.merge(category, stack.getCount(), Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .max(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparing(entry -> entry.getKey(), Comparator.reverseOrder()))
                .map(Map.Entry::getKey)
                .orElse(DEFAULT_FAILURE_CATEGORY);
    }
}
