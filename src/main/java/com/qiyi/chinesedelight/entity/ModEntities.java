package com.qiyi.chinesedelight.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.qiyi.chinesedelight.ChineseDelight;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, ChineseDelight.MODID);

    // 1.21.1 的 EntityType.Builder#build 接受 String（注册 id 的路径），不是 ResourceKey。
    public static final DeferredHolder<EntityType<?>, EntityType<MillKnotEntity>> MILL_KNOT =
            ENTITY_TYPES.register("mill_knot", id -> EntityType.Builder.<MillKnotEntity>of(MillKnotEntity::new, MobCategory.MISC)
                    .sized(0.0F, 0.0F)
                    .clientTrackingRange(10)
                    .updateInterval(Integer.MAX_VALUE)
                    .noSummon()
                    .build(id.getPath()));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}
