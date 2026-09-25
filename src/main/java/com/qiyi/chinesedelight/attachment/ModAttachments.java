package com.qiyi.chinesedelight.attachment;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.qiyi.chinesedelight.ChineseDelight;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

public final class ModAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ChineseDelight.MODID);

    @SuppressWarnings("unchecked")
    private static final Codec<Set<ResourceLocation>> DISH_SET_CODEC =
            ResourceLocation.CODEC.listOf().xmap(list -> (Set<ResourceLocation>) new HashSet<>(list), ArrayList::new);

    /** 玩家已经做出来过的菜品。厨师帽 HUD 用它决定显示菜名还是“？？？”。 */
    public static final Supplier<AttachmentType<Set<ResourceLocation>>> DISCOVERED_DISHES =
            ATTACHMENT_TYPES.register("discovered_dishes", () -> AttachmentType
                    .<Set<ResourceLocation>>builder(() -> new HashSet<>())
                    .serialize(DISH_SET_CODEC)
                    .copyOnDeath()
                    .build());

    private ModAttachments() {
    }

    public static void register(IEventBus modEventBus) {
        ATTACHMENT_TYPES.register(modEventBus);
    }
}
