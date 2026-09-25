package com.qiyi.chinesedelight.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.minecraft.core.BlockPos;
import com.qiyi.chinesedelight.attachment.ModAttachments;
import com.qiyi.chinesedelight.cooking.CookingClientState;

import java.util.List;
import java.util.Set;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetwork::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(DiscoveredDishesPayload.TYPE, DiscoveredDishesPayload.STREAM_CODEC,
                ModNetwork::onDiscoveredDishes);
    }

    private static void onDiscoveredDishes(DiscoveredDishesPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CookingClientState.setDiscovered(payload.dishes()));
    }

    /** 把某个玩家的“已做出菜品”同步给他自己。 */
    public static void syncDiscovered(ServerPlayer player) {
        Set<ResourceLocation> dishes = player.getData(ModAttachments.DISCOVERED_DISHES);
        PacketDistributor.sendToPlayer(player, new DiscoveredDishesPayload(List.copyOf(dishes)));
    }

    /** 石磨/铁锅做出菜以后，给附近的玩家记一笔“见过这道菜”。 */
    public static void awardDish(ServerLevel level, BlockPos pos, ResourceLocation dishId, Component dishName) {
        for (ServerPlayer player : level.getEntitiesOfClass(ServerPlayer.class, new AABB(pos).inflate(8.0))) {
            Set<ResourceLocation> dishes = player.getData(ModAttachments.DISCOVERED_DISHES);
            if (dishes.add(dishId)) {
                player.setData(ModAttachments.DISCOVERED_DISHES, dishes);
                syncDiscovered(player);
                player.displayClientMessage(
                        Component.translatable("message.chinesedelight.new_dish", dishName), false);
            }
        }
    }
}
