package com.qiyi.chinesedelight.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 客户端设置（{@code config/chinesedelight-client.toml}）。
 *
 * <p>只放纯客户端表现相关的东西，所以是 {@code ModConfig.Type.CLIENT}，不会同步到服务器。
 */
public final class HudConfig {
    public static final ModConfigSpec SPEC;

    /** 总开关：戴厨师帽时是否显示锅的抬头显示。 */
    public static final ModConfigSpec.BooleanValue ENABLED;
    /** 悬浮窗整体缩放，1.0 为原始大小。 */
    public static final ModConfigSpec.DoubleValue SCALE;
    /** 悬浮窗透明度（0.7 ≈ 30% 透明）。 */
    public static final ModConfigSpec.DoubleValue OPACITY;
    /** 面板相对锅在屏幕上的位置再偏移多少，得到"右上方"的效果。 */
    public static final ModConfigSpec.IntValue ANCHOR_OFFSET_X;
    public static final ModConfigSpec.IntValue ANCHOR_OFFSET_Y;
    /** 投影不可用时面板退回到"屏幕中心 + 偏移"，和 Create 护目镜一致。 */
    public static final ModConfigSpec.IntValue OFFSET_X;
    public static final ModConfigSpec.IntValue OFFSET_Y;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Chef hat cooking HUD (抬头显示)").push("hud");
        ENABLED = builder
                .comment("Show the cooking HUD when wearing the chef hat and looking at an iron pot.")
                .define("enabled", true);
        SCALE = builder
                .comment("Overall size of the floating panel. 1.0 = the panel's designed size.",
                        "The default is slightly below 1.0 so the panel and its text stay compact.")
                .defineInRange("scale", 0.8D, 0.4D, 2.5D);
        OPACITY = builder
                .comment("Panel opacity. 1.0 = fully opaque, 0.7 = 30% transparent.")
                .defineInRange("opacity", 0.7D, 0.1D, 1.0D);
        ANCHOR_OFFSET_X = builder
                .comment("Pixels to shift the panel right of the pot, so it sits at the pot's top-right.")
                .defineInRange("anchorOffsetX", 8, -200, 200);
        ANCHOR_OFFSET_Y = builder
                .comment("Pixels to lift the panel above the pot's anchor point.",
                        "Small positive values put it slightly above; negative values put it below.",
                        "The panel is clamped on screen, so it stays fully visible either way.")
                .defineInRange("anchorOffsetY", 8, -200, 200);
        OFFSET_X = builder
                .comment("Fallback screen-centre offset X, used only when the pot cannot be projected to screen.",
                        "Same idea as Create's engineer goggles overlay offset.")
                .defineInRange("overlayOffsetX", 0, -400, 400);
        OFFSET_Y = builder
                .comment("Fallback screen-centre offset Y, used only when the pot cannot be projected to screen.")
                .defineInRange("overlayOffsetY", 40, -400, 400);
        builder.pop();

        SPEC = builder.build();
    }

    private HudConfig() {
    }

    public static boolean enabled() {
        return ENABLED.getAsBoolean();
    }

    public static float scale() {
        return (float) (double) SCALE.getAsDouble();
    }

    public static int anchorOffsetX() {
        return ANCHOR_OFFSET_X.getAsInt();
    }

    public static int anchorOffsetY() {
        return ANCHOR_OFFSET_Y.getAsInt();
    }

    public static float opacity() {
        return (float) (double) OPACITY.getAsDouble();
    }

    public static int offsetX() {
        return OFFSET_X.getAsInt();
    }

    public static int offsetY() {
        return OFFSET_Y.getAsInt();
    }
}
