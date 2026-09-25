package com.qiyi.chinesedelight.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import com.qiyi.chinesedelight.ChineseDelight;
import com.qiyi.chinesedelight.block.custom.IronPotBlockEntity;
import com.qiyi.chinesedelight.item.ModItems;

/**
 * 把铁锅的位置投影到屏幕坐标，供 {@link CookingHudLayer} 画抬头显示用。
 *
 * <p>世界坐标 → 屏幕坐标需要 modelview / projection 矩阵，而这两个矩阵只在<b>渲染世界</b>时才有
 * （1.21.1 删掉了 {@code RenderSystem} 的矩阵读取接口）。所以这里在
 * {@link RenderLevelStageEvent.Stage#AFTER_ENTITIES} 阶段读
 * {@link RenderLevelStageEvent#getModelViewMatrix()} 与 {@link RenderLevelStageEvent#getProjectionMatrix()}
 * 做投影，只读不写，不会污染后续渲染。
 *
 * <p>此时 modelview 是"以相机为原点"的坐标系，所以被变换的向量也用相机相对坐标。
 */
@EventBusSubscriber(modid = ChineseDelight.MODID, value = Dist.CLIENT)
public final class CookingHudRenderer {
    /** 面板挂在锅口上方一点点，看起来像"贴在锅上"。 */
    private static final double ANCHOR_HEIGHT = 1.0D;

    private CookingHudRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        // 每帧都要上报"有没有在瞄锅"，否则 HUD 会卡在上一帧的状态
        if (!HudConfig.enabled()
                || !minecraft.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.CHEF_HAT.get())
                || !(minecraft.hitResult instanceof BlockHitResult blockHit)
                || !(minecraft.level.getBlockEntity(blockHit.getBlockPos()) instanceof IronPotBlockEntity)) {
            CookingHudState.set(false, false, 0.0F, 0.0F);
            return;
        }

        // 在瞄锅了，但投影可能失败（在背后 / 出画面）——那只是不跟随，面板仍会退到屏幕中心显示
        Camera camera = event.getCamera();
        Vec3 relative = Vec3.atCenterOf(blockHit.getBlockPos())
                .add(0.0D, ANCHOR_HEIGHT, 0.0D)
                .subtract(camera.getPosition());

        Vector4f clip = new Vector4f((float) relative.x, (float) relative.y, (float) relative.z, 1.0F);

        Matrix4f view = event.getModelViewMatrix();
        Matrix4f projection = event.getProjectionMatrix();
        view.transform(clip);
        projection.transform(clip);

        // w <= 0 表示这个点在相机背后
        if (clip.w <= 1.0E-4F) {
            CookingHudState.set(true, false, 0.0F, 0.0F);
            return;
        }

        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        float screenX = (clip.x / clip.w * 0.5F + 0.5F) * width;
        float screenY = (0.5F - clip.y / clip.w * 0.5F) * height;

        if (screenX < -256.0F || screenX > width + 256.0F
                || screenY < -256.0F || screenY > height + 256.0F) {
            CookingHudState.set(true, false, 0.0F, 0.0F);
            return;
        }

        CookingHudState.set(true, true, screenX, screenY);
    }
}
