package com.qiyi.chinesedelight.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.qiyi.chinesedelight.ChineseDelight;

import java.util.List;

/** 服务端 → 客户端：这个玩家已经做出过的菜品列表。 */
public record DiscoveredDishesPayload(List<ResourceLocation> dishes) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DiscoveredDishesPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ChineseDelight.MODID, "discovered_dishes"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DiscoveredDishesPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()),
                    DiscoveredDishesPayload::dishes,
                    DiscoveredDishesPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
