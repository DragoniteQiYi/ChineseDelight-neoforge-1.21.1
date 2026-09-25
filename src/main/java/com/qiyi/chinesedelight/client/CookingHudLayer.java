package com.qiyi.chinesedelight.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.joml.Matrix4f;
import com.qiyi.chinesedelight.ChineseDelight;
import com.qiyi.chinesedelight.block.ModBlocks;
import com.qiyi.chinesedelight.block.custom.IronPotBlockEntity;
import com.qiyi.chinesedelight.cooking.CookingClientState;
import com.qiyi.chinesedelight.cooking.CookingDataCache;
import com.qiyi.chinesedelight.cooking.CookingEvaluator;
import com.qiyi.chinesedelight.cooking.FlavorLookup;
import com.qiyi.chinesedelight.cooking.FlavorSet;
import com.qiyi.chinesedelight.cooking.FlavorType;
import com.qiyi.chinesedelight.item.ModItems;

/**
 * 厨师帽抬头显示。做法参考 Create 的工程师护目镜（{@code GoggleOverlayRenderer}）：
 * <ul>
 *     <li>同样是一个 {@link LayeredDraw.Layer}，在 GUI 阶段用 {@link GuiGraphics} 画；</li>
 *     <li>用"持续盯着多久"（{@code hoverTicks}）做<b>淡入 + 横向滑入</b>；</li>
 *     <li>面板 = 渐变底色 + 双色描边，再加一个 3D 物品图标做标题；</li>
 *     <li>面板会被夹在屏幕内，并且透明度取配置值。</li>
 * </ul>
 *
 * <p>和护目镜唯一的区别：锚点不是固定的"屏幕中心 + 偏移"，而是锅投影到屏幕上的位置
 * （见 {@link CookingHudRenderer}）；投影不可用时才退回屏幕中心偏移，行为与护目镜一致。
 */
public class CookingHudLayer implements LayeredDraw.Layer {
    /** 半透明像素底纹，叠在渐变底色上，避免纯色显得单调。 */
    private static final ResourceLocation PANEL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ChineseDelight.MODID, "textures/gui/cooking_hud.png");
    private static final int TEXTURE_SIZE = 48;
    private static final int SLICE = 6;

    private static final int PANEL_WIDTH = 108;
    private static final int HEADER_HEIGHT = 21;
    private static final int SECTION_HEIGHT = 10;
    private static final int ROW_HEIGHT = 11;
    private static final int FOOTER_HEIGHT = 14;
    /** 面板四周留白，标题和内容都从这里开始排。 */
    private static final int PAD = 6;

    private static final int COLUMN_WIDTH = 50;
    private static final int LABEL_WIDTH = 22;
    private static final int BAR_HEIGHT = 5;
    /** 一根进度条代表的味道值上限（单个食材最高 3~4，锅里 4 格，取 12 足够）。 */
    private static final int BAR_MAX = 12;
    /** 文字整体缩到 0.75，比原版字号小一圈；进度条和间距不受影响。 */
    private static final float TEXT_SCALE = 0.75F;

    // 浅色宣纸底 + 深色字（底纹见 textures/gui/cooking_hud.png）。
    // 这里写成不透明色，最终透明度统一由配置 opacity 控制，避免两层 alpha 相乘难以预期。
    private static final int COLOR_BG_TOP = 0xFFFBF7EC;
    private static final int COLOR_BG_BOTTOM = 0xFFF0E9D8;
    private static final int COLOR_BORDER_TOP = 0xFFB7A88D;
    private static final int COLOR_BORDER_BOTTOM = 0xFF6B5B45;
    private static final int COLOR_TITLE = 0xFF4A2E12;
    private static final int COLOR_TEXT = 0xFF3A2E20;
    private static final int COLOR_DIM = 0xFF7A6A52;
    private static final int COLOR_BAR_BACK = 0x33705A3C;
    private static final int COLOR_RESULT = 0xFF8A4B10;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (!HudConfig.enabled()) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CHEF_HAT.get())) {
            return;
        }
        if (!CookingHudState.isAimed()) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult blockHit)
                || !(minecraft.level.getBlockEntity(blockHit.getBlockPos()) instanceof IronPotBlockEntity pot)) {
            return;
        }

        FlavorLookup lookup = CookingDataCache.ingredients(minecraft.level.registryAccess());
        FlavorSet total = CookingEvaluator.accumulate(pot.contentList(), lookup);
        CookingEvaluator.Result result = pot.peekResult();

        int alpha = Math.round(HudConfig.opacity() * 255.0F);
        float scale = HudConfig.scale();
        int rows = (FlavorType.values().length + 1) / 2;
        int panelHeight = HEADER_HEIGHT + SECTION_HEIGHT + rows * ROW_HEIGHT + FOOTER_HEIGHT;

        // 与护目镜一致的淡入：前 24 tick 从侧面滑入并渐显
        float fade = CookingHudState.advance(deltaTracker.getGameTimeDeltaPartialTick(false));
        int slide = fade < 1.0F ? Math.round((float) Math.pow(1.0F - fade, 3.0D) * 8.0F) : 0;

        int guiWidth = graphics.guiWidth();
        int guiHeight = graphics.guiHeight();

        float anchorX;
        float anchorY;
        if (CookingHudState.hasProjection()) {
            // 面板挂到锅的右上方：左边缘在锅的右侧，上边缘比锚点略高一点
            anchorX = CookingHudState.screenX() + HudConfig.anchorOffsetX() * scale;
            anchorY = CookingHudState.screenY() - HudConfig.anchorOffsetY() * scale;
        } else {
            // 投影不可用时的兜底，等同护目镜的固定偏移
            anchorX = guiWidth / 2.0F + HudConfig.offsetX();
            anchorY = guiHeight / 2.0F + HudConfig.offsetY();
        }

        // 面板从锚点向右下方展开再夹进屏幕。
        // 注意这里必须整体夹住：玩家平视锅时锚点就在屏幕顶部附近，
        // 如果只夹上边缘，面板下半部分（整个味道列表）会被挤出画面，看起来像"面板没有内容"。
        float fullWidth = PANEL_WIDTH * scale;
        float fullHeight = panelHeight * scale;
        float leftF = Mth.clamp(anchorX, 2.0F, Math.max(2.0F, guiWidth - fullWidth - 2.0F));
        float topF = Mth.clamp(anchorY, 2.0F, Math.max(2.0F, guiHeight - fullHeight - 2.0F));
        int left = Math.round(leftF) + slide;
        int top = Math.round(topF);

        graphics.pose().pushPose();
        graphics.pose().translate(left, top, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);

        drawBackground(graphics, 0, 0, PANEL_WIDTH, panelHeight, fade, alpha);

        // 文字单独缩一圈；进度条和行距不动，所以排版不会被压乱
        graphics.pose().pushPose();
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, 1.0F);
        drawHeader(graphics, minecraft, pot, alpha);
        drawFlavors(graphics, minecraft, total, pot.isEmpty(), HEADER_HEIGHT, alpha);
        drawResult(graphics, minecraft, result, PANEL_WIDTH / 2,
                HEADER_HEIGHT + SECTION_HEIGHT + rows * ROW_HEIGHT + 3, alpha);
        graphics.pose().popPose();

        graphics.pose().popPose();
    }

    /**
     * 渐变底 + 描边，和 Create 的 tooltip 画法一致：先用 fillGradient 铺底，
     * 再画四边（上/下用同色、左/右做成上亮下暗的渐变），最后叠一层半透明像素底纹。
     *
     * <p><b>关键</b>：这里所有元素都必须用 {@code z = 0}。{@code GuiGraphics#fill} 不带 z 的重载
     * 就是 0，而 z 越大越靠前；如果给背景传了 400，背景会盖在 z=0 的文字和进度条上面，
     * 面板就会变成一块什么都没有的底板。所以这里只靠<b>绘制顺序</b>分层，不靠 z。
     */
    private static void drawBackground(GuiGraphics graphics, int x, int y, int width, int height,
                                       float fade, int alpha) {
        int bgTop = withAlpha(COLOR_BG_TOP, alpha);
        int bgBottom = withAlpha(COLOR_BG_BOTTOM, alpha);
        int borderTop = withAlpha(COLOR_BORDER_TOP, alpha);
        int borderBottom = withAlpha(COLOR_BORDER_BOTTOM, alpha);
        final int z = 0;

        // 底
        graphics.fillGradient(x, y, x + width, y + height, z, bgTop, bgBottom);

        // 半透明像素底纹（淡入时一起渐显）。压得比较淡，免得盖住文字和进度条
        int textureAlpha = Math.round(fade * 0x22);
        if (textureAlpha > 0) {
            drawPanelTexture(graphics, x, y, width, height, textureAlpha);
        }

        // 四边描边，1px
        graphics.fillGradient(x - 1, y - 1, x + width + 1, y, z, borderTop, borderTop);
        graphics.fillGradient(x - 1, y + height, x + width + 1, y + height + 1, z, borderBottom, borderBottom);
        graphics.fillGradient(x - 1, y, x, y + height, z, borderTop, borderBottom);
        graphics.fillGradient(x + width, y, x + width + 1, y + height, z, borderTop, borderBottom);
    }

    /** 九宫格贴底纹：四角与边框保持像素比例，只有中段被拉伸。 */
    private static void drawPanelTexture(GuiGraphics graphics, int x, int y, int width, int height, int textureAlpha) {
        int innerW = width - SLICE * 2;
        int innerH = height - SLICE * 2;
        int far = TEXTURE_SIZE - SLICE;

        blitSlice(graphics, x, y, SLICE, SLICE, 0, 0, SLICE, SLICE);
        blitSlice(graphics, x + width - SLICE, y, SLICE, SLICE, far, 0, SLICE, SLICE);
        blitSlice(graphics, x, y + height - SLICE, SLICE, SLICE, 0, far, SLICE, SLICE);
        blitSlice(graphics, x + width - SLICE, y + height - SLICE, SLICE, SLICE, far, far, SLICE, SLICE);
        blitSlice(graphics, x + SLICE, y, innerW, SLICE, SLICE, 0, SLICE, SLICE);
        blitSlice(graphics, x + SLICE, y + height - SLICE, innerW, SLICE, SLICE, far, SLICE, SLICE);
        blitSlice(graphics, x, y + SLICE, SLICE, innerH, 0, SLICE, SLICE, SLICE);
        blitSlice(graphics, x + width - SLICE, y + SLICE, SLICE, innerH, far, SLICE, SLICE, SLICE);
        blitSlice(graphics, x + SLICE, y + SLICE, innerW, innerH, SLICE, SLICE, SLICE, SLICE);
    }

    private static void blitSlice(GuiGraphics graphics, int x, int y, int width, int height,
                                  int uvX, int uvY, int uvWidth, int uvHeight) {
        // GuiGraphics#blit 会直接绑定原始贴图（不走 GUI 图集），所以 textures/gui 下的独立 png 可用
        graphics.blit(PANEL_TEXTURE, x, y, width, height, uvX, uvY, uvWidth, uvHeight, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    /**
     * 文字在渲染时被 {@link #TEXT_SCALE} 缩小了，但布局坐标仍是不缩放的。
     * 所以任何"想让文字间隔 N 像素"的地方都要先乘 1/TEXT_SCALE，
     * 否则缩过的文字会挤在一起（标题压状态、状态压小节标题）。
     */
    private static int tl(int pixels) {
        return Math.round(pixels / TEXT_SCALE);
    }

    /** 标题行：3D 物品图标 + 名字 + 水量/状态，和护目镜的"图标 + 标题"布局一致。 */
    private static void drawHeader(GuiGraphics graphics, Minecraft minecraft, IronPotBlockEntity pot, int alpha) {
        ItemStack icon = new ItemStack(ModBlocks.IRON_POT.get());
        graphics.renderItem(icon, PAD, 3);

        graphics.drawString(minecraft.font, Component.translatable("hud.chinesedelight.pot.title"),
                PAD + tl(17), tl(6), withAlpha(COLOR_TITLE, alpha), false);

        Component status = Component.translatable("hud.chinesedelight.pot.status",
                pot.getWater(), IronPotBlockEntity.MAX_WATER,
                Component.translatable(pot.isCooking() ? "hud.chinesedelight.cooking" : "hud.chinesedelight.idle"));
        graphics.drawString(minecraft.font, status, PAD + tl(17), tl(16), withAlpha(COLOR_DIM, alpha), false);
    }

    /** 十项味道两列排开，每项一条彩色进度条；锅里没东西时给一句提示，避免面板看起来是空的。 */
    private static void drawFlavors(GuiGraphics graphics, Minecraft minecraft, FlavorSet total, boolean potEmpty,
                                    int top, int alpha) {
        graphics.drawString(minecraft.font, Component.translatable("hud.chinesedelight.flavors"),
                PAD, top + tl(2), withAlpha(COLOR_DIM, alpha), false);

        int listTop = top + SECTION_HEIGHT;
        if (potEmpty) {
            graphics.drawString(minecraft.font, Component.translatable("hud.chinesedelight.pot.empty"),
                    PAD, listTop + tl(2), withAlpha(COLOR_TEXT, alpha), false);
            return;
        }

        FlavorType[] types = FlavorType.values();
        for (int i = 0; i < types.length; i++) {
            int x = PAD + (i % 2) * COLUMN_WIDTH;
            int y = listTop + (i / 2) * ROW_HEIGHT;
            drawFlavorBar(graphics, minecraft, types[i], total.get(types[i]), x, y, alpha);
        }
    }

    private static void drawFlavorBar(GuiGraphics graphics, Minecraft minecraft, FlavorType type, int value,
                                      int x, int y, int alpha) {
        // 标签在缩放后的文字空间里，进度条和它在同一个视觉高度
        graphics.drawString(minecraft.font, flavorLabel(minecraft, type), x, y,
                withAlpha(value > 0 ? COLOR_TEXT : COLOR_DIM, alpha), false);

        int barY = y + tl(4);
        int barHeight = Math.max(2, Math.round(BAR_HEIGHT * TEXT_SCALE));
        int barX = x + LABEL_WIDTH;
        int barWidth = COLUMN_WIDTH - LABEL_WIDTH - 13;

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, withAlpha(COLOR_BAR_BACK, alpha));
        if (value > 0) {
            int filled = Math.max(2, Math.min(barWidth, Math.round(barWidth * (value / (float) BAR_MAX))));
            graphics.fill(barX, barY, barX + filled, barY + barHeight, withAlpha(flavorColor(type), alpha));
        }

        graphics.drawString(minecraft.font, Integer.toString(value),
                barX + barWidth + 3, y, withAlpha(value > 0 ? COLOR_TEXT : COLOR_DIM, alpha), false);
    }

    /** 每个味道一个固定颜色，尽量和它的"味觉印象"对得上；整体压深一点，配浅色底。 */
    private static int flavorColor(FlavorType type) {
        return switch (type) {
            case VEGETABLE -> 0xFF4E8A28; // 绿
            case SWEET -> 0xFFC8507F;     // 玫红
            case SALTY -> 0xFF3F72B0;     // 蓝
            case SOUR -> 0xFF9A9410;      // 黄绿
            case BITTER -> 0xFF6B5233;    // 褐
            case SPICY -> 0xFFC02A18;     // 红
            case UMAMI -> 0xFFA86220;     // 琥珀
            case NUMBING -> 0xFF7546B8;   // 紫
            case OILY -> 0xFFB8860B;      // 暗金
            case STARCHY -> 0xFF94795A;   // 米褐
        };
    }

    /** 中文标签很短，英文原版名太长会把进度条挤没，所以英文下用 4 字以内的缩写。 */
    private static String flavorLabel(Minecraft minecraft, FlavorType type) {
        if ("en_us".equals(minecraft.getLanguageManager().getSelected())) {
            return switch (type) {
                case VEGETABLE -> "Veg";
                case SWEET -> "Swt";
                case SALTY -> "Slt";
                case SOUR -> "Sour";
                case BITTER -> "Bitt";
                case SPICY -> "Spcy";
                case UMAMI -> "Umam";
                case NUMBING -> "Numb";
                case OILY -> "Oil";
                case STARCHY -> "Stch";
            };
        }
        return Component.translatable(type.getTranslationKey()).getString();
    }

    private static void drawResult(GuiGraphics graphics, Minecraft minecraft, CookingEvaluator.Result result,
                                   int centerX, int y, int alpha) {
        Component dish = result == null
                ? Component.translatable("hud.chinesedelight.nothing")
                : (result.failure()
                        ? result.dish().result().getHoverName().copy().withStyle(ChatFormatting.RED)
                        : (CookingClientState.isDiscovered(result.id())
                                ? result.dish().result().getHoverName()
                                : Component.translatable("hud.chinesedelight.unknown").withStyle(ChatFormatting.OBFUSCATED)));

        graphics.drawCenteredString(minecraft.font, Component.translatable("hud.chinesedelight.pot.result", dish),
                centerX, y, withAlpha(COLOR_RESULT, alpha));
    }

    /** 把配置里的不透明度乘进颜色的 alpha 通道。 */
    private static int withAlpha(int argb, int alpha) {
        int combined = (argb >>> 24) * alpha / 255;
        return (combined << 24) | (argb & 0x00FFFFFF);
    }
}
