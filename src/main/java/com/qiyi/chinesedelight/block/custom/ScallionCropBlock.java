package com.qiyi.chinesedelight.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.qiyi.chinesedelight.item.ModItems;

/**
 * 葱：只有三个阶段。
 * <ul>
 *     <li>age=0 刚种下，打掉只收回葱种子</li>
 *     <li>age=1 打掉收获小葱</li>
 *     <li>age=2 打掉收获大葱</li>
 * </ul>
 * 生长速度约为普通作物的两倍。
 */
public class ScallionCropBlock extends CropBlock {
    public static final MapCodec<ScallionCropBlock> CODEC = simpleCodec(ScallionCropBlock::new);
    public static final int MAX_AGE = 2;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
    // 1.21.1 没有 Block#boxes/Block#column，按阶段手写一张表。
    private static final VoxelShape[] SHAPES = createShapes();

    private static VoxelShape[] createShapes() {
        VoxelShape[] shapes = new VoxelShape[MAX_AGE + 1];
        for (int age = 0; age <= MAX_AGE; age++) {
            shapes[age] = Block.box(0.0, 0.0, 0.0, 16.0, 4.0 + age * 5.0, 16.0);
        }
        return shapes;
    }

    public ScallionCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ScallionCropBlock> codec() {
        return CODEC;
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return MAX_AGE;
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return ModItems.SCALLION_SEED;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // 生长快：一次随机刻里跑两遍原版生长判定
        super.randomTick(state, level, pos, random);
        BlockState grown = level.getBlockState(pos);
        if (grown.is(this) && !this.isMaxAge(grown)) {
            super.randomTick(grown, level, pos, random);
        }
    }

    /** 骨粉一次只催熟一个阶段（原版默认一次推进 2~5 个阶段）。 */
    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return 1;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[this.getAge(state)];
    }
}
