package com.qiyi.chinesedelight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import com.qiyi.chinesedelight.block.custom.IronPotBlockEntity;

import java.util.List;

/** 把锅里的 4 格食材画在锅口上方转圈，方便玩家一眼看到放了什么。 */
public class IronPotRenderer implements BlockEntityRenderer<IronPotBlockEntity> {

    /** 1.21.1 的 render 没有 cameraPos 参数（那是 1.21.5 加的）。 */
    @Override
    public void render(IronPotBlockEntity pot, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {
        List<ItemStack> contents = pot.contentList();
        if (contents.stream().allMatch(ItemStack::isEmpty)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        long time = pot.getLevel() == null ? 0L : pot.getLevel().getGameTime();
        float spin = (time + partialTick) * 2.0F;

        for (int i = 0; i < contents.size(); i++) {
            ItemStack stack = contents.get(i);
            if (stack.isEmpty()) {
                continue;
            }
            double angle = Math.PI * 2.0 * i / IronPotBlockEntity.SLOTS + Math.toRadians(spin);
            float bob = Mth.sin((time + partialTick) * 0.1F + i) * 0.02F;

            poseStack.pushPose();
            poseStack.translate(0.5 + Math.cos(angle) * 0.22, 0.72 + bob, 0.5 + Math.sin(angle) * 0.22);
            poseStack.scale(0.3F, 0.3F, 0.3F);
            poseStack.mulPose(Axis.YP.rotationDegrees(spin + i * 45.0F));
            minecraft.getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND, packedLight, packedOverlay,
                    poseStack, bufferSource, pot.getLevel(), (int) (pot.getBlockPos().asLong() + i));
            poseStack.popPose();
        }
    }
}
