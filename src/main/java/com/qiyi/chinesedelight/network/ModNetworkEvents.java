package com.qiyi.chinesedelight.network;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.qiyi.chinesedelight.ChineseDelight;

/** 玩家进服时把他“做过哪些菜”同步过去（厨师帽 HUD 要用）。 */
@EventBusSubscriber(modid = ChineseDelight.MODID)
public final class ModNetworkEvents {

    private ModNetworkEvents() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            ModNetwork.syncDiscovered(player);
        }
    }
}
