package com.qiyi.chinesedelight.cooking;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 某个味觉轴的要求区间。JSON 里可以直接写整数（表示“至少这么多”），也可以写 {@code {"min": 2, "max": 4}}。
 */
public record FlavorRange(int min, int max) {
    public static final FlavorRange ANY = new FlavorRange(0, Integer.MAX_VALUE);

    private static final Codec<FlavorRange> FULL = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("min", 0).forGetter(FlavorRange::min),
            Codec.INT.optionalFieldOf("max", Integer.MAX_VALUE).forGetter(FlavorRange::max)
    ).apply(instance, FlavorRange::new));

    public static final Codec<FlavorRange> CODEC = Codec.either(Codec.INT, FULL).xmap(
            either -> either.map(min -> new FlavorRange(min, Integer.MAX_VALUE), range -> range),
            range -> range.max() == Integer.MAX_VALUE ? Either.left(range.min()) : Either.right(range));

    public boolean test(int value) {
        return value >= this.min && value <= this.max;
    }

    public boolean isAny() {
        return this.min <= 0 && this.max == Integer.MAX_VALUE;
    }
}
