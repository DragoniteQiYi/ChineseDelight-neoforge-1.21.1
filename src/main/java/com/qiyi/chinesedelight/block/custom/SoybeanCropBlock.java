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
 * 大豆：四个生长阶段，用黄豆种植。
 * <ul>
 *     <li>age=0/1 打掉收回黄豆</li>
 *     <li>age=2 打掉收获毛豆</li>
 *     <li>age=3（成熟）打掉收获黄豆</li>
 * </ul>
 */
public class SoybeanCropBlock extends CropBlock {
    public static final MapCodec<SoybeanCropBlock> CODEC = simpleCodec(SoybeanCropBlock::new);
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

    public SoybeanCropBlock(Properties properties) {
        super(properties);
    }

    @Override
    public MapCodec<SoybeanCropBlock> codec() {
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
        return ModItems.SOYBEAN;
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
