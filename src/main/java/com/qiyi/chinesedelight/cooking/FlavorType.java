package com.qiyi.chinesedelight.cooking;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;

/** 食材的十个数值轴：蔬菜 + 六味 + 麻 + 油 + 淀粉。 */
public enum FlavorType implements StringRepresentable {
    VEGETABLE("vegetable"),
    SWEET("sweet"),
    SALTY("salty"),
    SOUR("sour"),
    BITTER("bitter"),
    SPICY("spicy"),
    UMAMI("umami"),
    NUMBING("numbing"),
    OILY("oily"),
    STARCHY("starchy");

    public static final Codec<FlavorType> CODEC = StringRepresentable.fromEnum(FlavorType::values);

    private final String name;

    FlavorType(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** 显示用的翻译键。 */
    public String getTranslationKey() {
        return "flavor.chinesedelight." + this.name;
    }
}
