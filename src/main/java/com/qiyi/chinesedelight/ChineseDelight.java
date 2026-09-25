package com.qiyi.chinesedelight;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import com.qiyi.chinesedelight.attachment.ModAttachments;
import com.qiyi.chinesedelight.block.ModBlockEntities;
import com.qiyi.chinesedelight.block.ModBlocks;
import com.qiyi.chinesedelight.client.HudConfig;
import com.qiyi.chinesedelight.cooking.ModCookingRegistries;
import com.qiyi.chinesedelight.entity.ModEntities;
import com.qiyi.chinesedelight.item.ModCreativeTabs;
import com.qiyi.chinesedelight.item.ModItems;
import com.qiyi.chinesedelight.network.ModNetwork;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ChineseDelight.MODID)
public class ChineseDelight
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "chinesedelight";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ChineseDelight(IEventBus modEventBus, ModContainer modContainer)
    {
        // Register the Deferred Registers of every registry this mod adds entries to
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModCookingRegistries.register(modEventBus);
        ModAttachments.register(modEventBus);
        ModNetwork.register(modEventBus);

        // 客户端设置：厨师帽抬头显示的缩放 / 位置 / 透明度，只影响本机表现
        modContainer.registerConfig(ModConfig.Type.CLIENT, HudConfig.SPEC);

        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("中华乐事 (Chinese Delight) is loading");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("Chinese Delight: server starting");
    }
}
