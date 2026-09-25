package com.qiyi.chinesedelight.block;

import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.qiyi.chinesedelight.ChineseDelight;
import com.qiyi.chinesedelight.block.custom.ChiliCropBlock;
import com.qiyi.chinesedelight.block.custom.FermentingJarBlock;
import com.qiyi.chinesedelight.block.custom.GarlicCropBlock;
import com.qiyi.chinesedelight.block.custom.GingerCropBlock;
import com.qiyi.chinesedelight.block.custom.IronPanBlock;
import com.qiyi.chinesedelight.block.custom.IronPotBlock;
import com.qiyi.chinesedelight.block.custom.NapaCabbageCropBlock;
import com.qiyi.chinesedelight.block.custom.PeppercornCropBlock;
import com.qiyi.chinesedelight.block.custom.ScallionCropBlock;
import com.qiyi.chinesedelight.block.custom.SoybeanCropBlock;
import com.qiyi.chinesedelight.block.custom.StoneMillBlock;
import com.qiyi.chinesedelight.item.ModItems;

import java.util.function.Function;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(ChineseDelight.MODID);

    public static final DeferredBlock<Block> SALT_ORE = registerBlock("saltore",
            props -> new DropExperienceBlock(UniformInt.of(0, 1),
                    props.strength(2F).requiresCorrectToolForDrops().sound(SoundType.STONE)));

    public static final DeferredBlock<IronPanBlock> IRON_PAN = registerBlock("ironpan",
            props -> new IronPanBlock(props.noOcclusion().strength(2F).sound(SoundType.METAL)));

    public static final DeferredBlock<IronPotBlock> IRON_POT = registerBlock("ironpot",
            props -> new IronPotBlock(props.noOcclusion().strength(2F).sound(SoundType.METAL)));

    // 农作物没有对应的方块物品，种植用的是种子物品（见 ModItems）
    public static final DeferredBlock<ScallionCropBlock> SCALLION_CROP = BLOCKS.registerBlock("scallioncrop",
            props -> new ScallionCropBlock(cropProperties(props)));

    public static final DeferredBlock<GingerCropBlock> GINGER_CROP = BLOCKS.registerBlock("gingercrop",
            props -> new GingerCropBlock(cropProperties(props)));

    public static final DeferredBlock<GarlicCropBlock> GARLIC_CROP = BLOCKS.registerBlock("garliccrop",
            props -> new GarlicCropBlock(cropProperties(props)));

    public static final DeferredBlock<NapaCabbageCropBlock> NAPA_CABBAGE_CROP = BLOCKS.registerBlock("napacabbagecrop",
            props -> new NapaCabbageCropBlock(cropProperties(props)));

    public static final DeferredBlock<SoybeanCropBlock> SOYBEAN_CROP = BLOCKS.registerBlock("soybeancrop",
            props -> new SoybeanCropBlock(cropProperties(props)));

    public static final DeferredBlock<PeppercornCropBlock> PEPPERCORN_CROP = BLOCKS.registerBlock("peppercorncrop",
            props -> new PeppercornCropBlock(cropProperties(props)));

    public static final DeferredBlock<ChiliCropBlock> CHILI_CROP = BLOCKS.registerBlock("chilicrop",
            props -> new ChiliCropBlock(cropProperties(props)));

    // 陶缸：发酵 / 腌制 / 和面
    public static final DeferredBlock<FermentingJarBlock> FERMENTING_JAR = registerBlock("fermentingjar",
            props -> new FermentingJarBlock(props.mapColor(MapColor.TERRACOTTA_BROWN)
                    .strength(1.5F)
                    .sound(SoundType.DECORATED_POT)));

    // 石磨：不用 GUI，直接对着方块操作
    public static final DeferredBlock<StoneMillBlock> STONE_MILL = registerBlock("stonemill",
            props -> new StoneMillBlock(props.mapColor(MapColor.STONE)
                    .strength(3.5F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.STONE)
                    .noOcclusion()));

    private static BlockBehaviour.Properties cropProperties(BlockBehaviour.Properties props) {
        return props.mapColor(MapColor.PLANT)
                .noCollission()
                .randomTicks()
                .instabreak()
                .sound(SoundType.CROP)
                .pushReaction(PushReaction.DESTROY);
    }

    /**
     * Registers a block and its {@link net.minecraft.world.item.BlockItem} under the same name.
     * The registry ids are filled in by the deferred register itself.
     */
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Function<BlockBehaviour.Properties, ? extends T> factory) {
        DeferredBlock<T> block = BLOCKS.registerBlock(name, factory);
        ModItems.ITEMS.registerSimpleBlockItem(name, block);
        return block;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
