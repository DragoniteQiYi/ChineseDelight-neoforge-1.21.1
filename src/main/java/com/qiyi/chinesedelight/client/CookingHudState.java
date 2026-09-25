package com.qiyi.chinesedelight.client;

import net.minecraft.util.Mth;

/**
 * 厨师帽抬头显示的每帧状态，思路照搬 Create 的工程师护目镜（{@code GoggleOverlayRenderer}）：
 * 用 {@code hoverTicks} 累积"持续盯着同一个方块多久"，再据此做淡入 + 横向滑入。
 *
 * <p>唯一比 Create 多出来的是 {@link #screenX()}/{@link #screenY()}：护目镜的面板固定在屏幕中心偏置处，
 * 而这里要跟着锅在视角中的位置走，所以多存一份投影结果。投影在世界渲染阶段算好，
 * 按时间戳判断新鲜度（不做同帧强绑，避免受渲染顺序影响）。
 */
public final class CookingHudState {
    /** 超过这个时长没拿到新投影就认为锅已经不在视野里了。 */
    private static final long STALE_NANOS = 250_000_000L;
    /** Create 用的淡入时长：24 tick。 */
    private static final float FADE_TICKS = 24.0F;

    private static boolean aimed;
    private static boolean projected;
    private static long projectedAtNanos = Long.MIN_VALUE;
    private static float screenX;
    private static float screenY;
    private static int hoverTicks;

    private CookingHudState() {
    }

    /** 世界渲染阶段每帧调用：是否正瞄着锅，以及锅（若在视野内）投影到的屏幕坐标。 */
    static void set(boolean aimedNow, boolean projectedNow, float x, float y) {
        aimed = aimedNow;
        if (aimedNow && projectedNow) {
            projected = true;
            screenX = x;
            screenY = y;
            projectedAtNanos = System.nanoTime();
        } else {
            projected = false;
        }
    }

    /** GUI 阶段每帧调用，推进淡入进度；返回当前 0~1 的淡入系数。 */
    public static float advance(float partialTick) {
        if (aimed) {
            hoverTicks++;
        } else {
            hoverTicks = 0;
        }
        return Mth.clamp((hoverTicks + partialTick) / FADE_TICKS, 0.0F, 1.0F);
    }

    /** 是否正在瞄着锅（淡入还没到 1 也要显示，否则永远淡不进来）。 */
    public static boolean isAimed() {
        return aimed;
    }

    /** 投影结果是否新鲜可用；不可用时退回到"屏幕中心 + 配置偏移"，和 Create 一样。 */
    public static boolean hasProjection() {
        return projected
                && projectedAtNanos != Long.MIN_VALUE
                && System.nanoTime() - projectedAtNanos < STALE_NANOS;
    }

    public static float screenX() {
        return screenX;
    }

    public static float screenY() {
        return screenY;
    }
}
