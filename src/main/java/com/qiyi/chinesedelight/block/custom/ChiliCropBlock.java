package com.qiyi.chinesedelight.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
 * 辣椒：四个生长阶段，用辣椒种子种植，成熟后收获辣椒（辣味与豆瓣酱的原料）。
 */
public class ChiliCropBlock extends CropBlock {
    public static final MapCodec<ChiliCropBlock> CODEC = simpleCodec(ChiliCropBlock::new);
    public static final int MAX_AGE = 3;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    // 1.21.1 没有 Block#boxes/Block#column，按阶段手写一张表。
    private static final VoxelShape[] SHAPES = createShapes();

    private static VoxelShape[] createShapes() {
        VoxelShape[] shapes = new VoxelShape[MAX_AGE + 1];
        for (int age = 0; age <= MAX_AGE; age++) {
            shapes[age] = Block.box(0.0, 0.0, 0.0, 16.0, 3.0 + age * 3.0, 16.0);
        }
        return shapes;
    }

    public ChiliCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<ChiliCropBlock> codec() {
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
        return ModItems.CHILI_SEED;
    }

    /** 骨粉一次只催熟一个阶段（和其它作物一致）。 */
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
