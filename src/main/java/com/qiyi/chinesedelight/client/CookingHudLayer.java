package com.qiyi.chinesedelight.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import com.qiyi.chinesedelight.block.custom.IronPotBlockEntity;
import com.qiyi.chinesedelight.cooking.CookingClientState;
import com.qiyi.chinesedelight.cooking.CookingDataCache;
import com.qiyi.chinesedelight.cooking.CookingEvaluator;
import com.qiyi.chinesedelight.cooking.FlavorLookup;
import com.qiyi.chinesedelight.cooking.FlavorSet;
import com.qiyi.chinesedelight.cooking.FlavorType;
import com.qiyi.chinesedelight.item.ModItems;

import java.util.Map;

/**
 * 厨师帽 HUD：戴着帽子看铁锅时，显示锅内每样食材的味道、合计味道，以及预计会出锅的菜。
 * 没做过的菜只显示“？？？”，做过一次才会显示名字。
 */
public class CookingHudLayer implements LayeredDraw.Layer {
    private static final int PANEL_COLOR = 0xC8141414;
    private static final int TITLE_COLOR = 0xFFF0C060;
    private static final int TEXT_COLOR = 0xFFEDEDED;
    private static final int DIM_COLOR = 0xFF9A9A9A;
    private static final int FLAVOR_COLOR = 0xFF9CCF6A;

    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        if (!player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CHEF_HAT.get())) {
            return;
        }
        if (!(minecraft.hitResult instanceof BlockHitResult blockHit)) {
            return;
        }
        if (blockHit.getBlockPos().distToCenterSqr(player.position()) > 100.0) {
            return;
        }
        if (!(minecraft.level.getBlockEntity(blockHit.getBlockPos()) instanceof IronPotBlockEntity pot)) {
            return;
        }

        FlavorLookup lookup = CookingDataCache.ingredients(minecraft.level.registryAccess());
        FlavorSet total = CookingEvaluator.accumulate(pot.contentList(), lookup);
        CookingEvaluator.Result result = pot.peekResult();

        int x = 6;
        int y = 6;
        int width = 210;
        int height = 34 + IronPotBlockEntity.SLOTS * 18 + 30;
        graphics.fill(x - 3, y - 3, x + width, y + height, PANEL_COLOR);

        graphics.drawString(minecraft.font, Component.translatable("hud.chinesedelight.pot.title"), x, y, TITLE_COLOR, true);
        Component status = Component.translatable("hud.chinesedelight.pot.status",
                pot.getWater(), IronPotBlockEntity.MAX_WATER,
                Component.translatable(pot.isCooking() ? "hud.chinesedelight.cooking" : "hud.chinesedelight.idle"));
        graphics.drawString(minecraft.font, status, x + 70, y, DIM_COLOR, true);

        int lineY = y + 16;
        boolean empty = true;
        for (ItemStack stack : pot.contentList()) {
            if (stack.isEmpty()) {
                continue;
            }
            empty = false;
            graphics.renderItem(stack, x, lineY - 4);
            graphics.drawString(minecraft.font, stack.getHoverName(), x + 20, lineY, TEXT_COLOR, true);
            graphics.drawString(minecraft.font, describe(lookup.flavorsOf(stack)), x + 118, lineY, FLAVOR_COLOR, true);
            lineY += 18;
        }
        if (empty) {
            graphics.drawString(minecraft.font, Component.translatable("hud.chinesedelight.pot.empty"), x + 4, lineY, DIM_COLOR, true);
            lineY += 18;
        }

        graphics.drawString(minecraft.font, Component.translatable("hud.chinesedelight.pot.total", describe(total)),
                x + 4, lineY + 4, 0xFFFFE080, true);
        lineY += 22;

        Component dish = result == null
                ? Component.translatable("hud.chinesedelight.nothing")
                : (result.failure()
                        ? result.dish().result().getHoverName().copy().withStyle(ChatFormatting.RED)
                        : (CookingClientState.isDiscovered(result.id())
                                ? result.dish().result().getHoverName()
                                : Component.translatable("hud.chinesedelight.unknown").withStyle(ChatFormatting.OBFUSCATED)));
        graphics.drawString(minecraft.font, Component.translatable("hud.chinesedelight.pot.result", dish),
                x + 4, lineY, TEXT_COLOR, true);
    }

    private static String describe(FlavorSet flavors) {
        if (flavors.isEmpty()) {
            return Component.translatable("hud.chinesedelight.no_flavor").getString();
        }
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<FlavorType, Integer> entry : flavors.asMap().entrySet()) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Component.translatable(entry.getKey().getTranslationKey()).getString())
                    .append(entry.getValue());
        }
        return builder.toString();
    }
}
