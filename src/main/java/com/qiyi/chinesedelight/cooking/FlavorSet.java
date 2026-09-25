package com.qiyi.chinesedelight.cooking;

import com.mojang.serialization.Codec;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * 一组味道数值。只保存非 0 的轴，迭代顺序固定为 {@link FlavorType} 的声明顺序。
 */
public final class FlavorSet {
    public static final FlavorSet EMPTY = new FlavorSet(Map.of());
    public static final Codec<FlavorSet> CODEC =
            Codec.unboundedMap(FlavorType.CODEC, Codec.INT).xmap(FlavorSet::new, FlavorSet::asMap);

    private final Map<FlavorType, Integer> values;

    private FlavorSet(Map<FlavorType, Integer> values) {
        EnumMap<FlavorType, Integer> map = new EnumMap<>(FlavorType.class);
        values.forEach((type, value) -> {
            if (value != null && value != 0) {
                map.put(type, value);
            }
        });
        this.values = Collections.unmodifiableMap(map);
    }

    public static FlavorSet of(FlavorType type, int value) {
        return new FlavorSet(Map.of(type, value));
    }

    public static FlavorSet copyOf(Map<FlavorType, Integer> values) {
        return new FlavorSet(values);
    }

    public int get(FlavorType type) {
        return this.values.getOrDefault(type, 0);
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    public FlavorSet plus(FlavorSet other) {
        if (other.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return other;
        }
        EnumMap<FlavorType, Integer> map = new EnumMap<>(this.values);
        other.values.forEach((type, value) -> map.merge(type, value, Integer::sum));
        return new FlavorSet(map);
    }

    public Map<FlavorType, Integer> asMap() {
        return this.values;
    }

    @Override
    public String toString() {
        return this.values.toString();
    }
}
