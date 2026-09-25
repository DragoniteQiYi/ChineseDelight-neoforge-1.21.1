package com.qiyi.chinesedelight.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.qiyi.chinesedelight.ChineseDelight;
import com.qiyi.chinesedelight.block.custom.FermentingJarBlockEntity;
import com.qiyi.chinesedelight.block.custom.IronPotBlockEntity;
import com.qiyi.chinesedelight.block.custom.StoneMillBlockEntity;

import java.util.Set;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ChineseDelight.MODID);

    // 1.21.1 的 BlockEntityType 只有 Builder.of(...).build(Type)，公开构造器要多一个 dataType 参数。
    // 用 BlockEntitySupplier<T>（而不是 ? extends T）才能让 Builder.of 推断出同一类型变量。
    private static <T extends BlockEntity> BlockEntityType<T> create(
            BlockEntityType.BlockEntitySupplier<T> factory, Block block) {
        return BlockEntityType.Builder.of(factory, block).build(null);
    }

    // The supplier runs while the block entity type registry is filled, which happens after blocks are registered,
    // so ModBlocks.IRON_POT is already bound at that point.
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronPotBlockEntity>> IRON_POT =
            BLOCK_ENTITIES.register("ironpot",
                    () -> create(IronPotBlockEntity::new, ModBlocks.IRON_POT.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<StoneMillBlockEntity>> STONE_MILL =
            BLOCK_ENTITIES.register("stonemill",
                    () -> create(StoneMillBlockEntity::new, ModBlocks.STONE_MILL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FermentingJarBlockEntity>> FERMENTING_JAR =
            BLOCK_ENTITIES.register("fermentingjar",
                    () -> create(FermentingJarBlockEntity::new, ModBlocks.FERMENTING_JAR.get()));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
