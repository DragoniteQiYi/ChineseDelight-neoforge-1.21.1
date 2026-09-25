package com.qiyi.chinesedelight.client;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.minecraft.client.renderer.entity.NoopRenderer;
import com.qiyi.chinesedelight.ChineseDelight;
import com.qiyi.chinesedelight.block.ModBlockEntities;
import com.qiyi.chinesedelight.entity.ModEntities;

// bus() 在 1.21.1 已被忽略并标记待删除：FML 会按事件是否实现 IModBusEvent 自动选总线。
// EntityRenderersEvent.RegisterRenderers 和 RegisterGuiLayersEvent 都实现了 IModBusEvent，
// 所以两者都会照常注册到 mod 事件总线。
@EventBusSubscriber(modid = ChineseDelight.MODID, value = Dist.CLIENT)
public class ModClientSetup {

    /** 石磨的隐形拴绳桩不需要渲染，注册一个空渲染器免得刷警告。 */
    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.MILL_KNOT.get(), NoopRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.IRON_POT.get(), context -> new IronPotRenderer());
    }

    /** 厨师帽 HUD。 */
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(ChineseDelight.MODID, "cooking_hud"),
                new CookingHudLayer());
    }
}
