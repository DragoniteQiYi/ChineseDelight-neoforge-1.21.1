package com.qiyi.chinesedelight.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import com.qiyi.chinesedelight.block.ModBlockEntities;

/**
 * 石磨方块。所有交互都在 {@link StoneMillBlockEntity} 里，没有 GUI。
 */
public class StoneMillBlock extends BaseEntityBlock {
    public static final MapCodec<StoneMillBlock> CODEC = simpleCodec(StoneMillBlock::new);
    /** 是否正在研磨，用来切换模型。 */
    public static final BooleanProperty GRINDING = BooleanProperty.create("grinding");
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 10.0, 16.0);

    public StoneMillBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(GRINDING, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRINDING);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StoneMillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.STONE_MILL.get(), StoneMillBlockEntity::serverTick);
    }

    // 1.21.1 的 useItemOn 返回 ItemInteractionResult，不再是 InteractionResult。
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof StoneMillBlockEntity mill)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // 牵着牛/马右键：拴到石磨上
        if (mill.bindAnimal(level, pos, player)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.WATER_BUCKET)) {
            return mill.addWater(level, pos, player, hand, stack);
        }
        if (stack.is(Items.BUCKET)) {
            return mill.takePortion(level, pos, player, hand, stack);
        }
        if (mill.insertByHand(level, pos, player, hand, stack).consumesAction()) {
            return ItemInteractionResult.SUCCESS;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty()) {
            // 手里拿着别的东西时不当作手动推磨，让物品照常使用
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof StoneMillBlockEntity mill)) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            // 潜行 + 空手：解开拴着的动物
            return mill.releaseAnimal(level, pos) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        return mill.grindManually(level, pos, player).result();
    }
}
