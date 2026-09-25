package com.qiyi.chinesedelight.item.custom;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public class IronKitchenKnife extends Item {

    public IronKitchenKnife(Properties properties) {
        super(properties);
    }

    /** 1.21.1 的签名是 (ItemStack, TooltipContext, List&lt;Component&gt;, TooltipFlag)，没有 1.21.5 的 TooltipDisplay。 */
    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        if (Screen.hasShiftDown()) {
            tooltipComponents.add(Component.translatable("tooltip.chinesedelight.ironkitchenknife").withStyle(ChatFormatting.YELLOW));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.chinesedelight.hold_shift").withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
