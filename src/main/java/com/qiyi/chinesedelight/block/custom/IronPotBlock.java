package com.qiyi.chinesedelight.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import com.qiyi.chinesedelight.block.ModBlockEntities;

/**
 * 铁锅方块。没有 GUI：食材丢进去，水桶右键加水，空手右键开火，潜行 + 空手右键退料。
 */
public class IronPotBlock extends BaseEntityBlock {
    public static final MapCodec<IronPotBlock> CODEC = simpleCodec(IronPotBlock::new);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 1.0, 15.0, 11.0, 15.0);

    public IronPotBlock(Properties properties) {
        super(properties);
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IronPotBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null
                : createTickerHelper(type, ModBlockEntities.IRON_POT.get(), IronPotBlockEntity::serverTick);
    }

    // 1.21.1 的 useItemOn 返回 ItemInteractionResult，不再是 InteractionResult。
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.is(Items.WATER_BUCKET) && level.getBlockEntity(pos) instanceof IronPotBlockEntity pot) {
            return pot.addWater(level, pos, player, hand, stack);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!(level.getBlockEntity(pos) instanceof IronPotBlockEntity pot)) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            return pot.ejectContents(level, pos).result();
        }
        if (pot.isCooking()) {
            int percent = pot.getCookProgress() * 100 / IronPotBlockEntity.COOK_TIME;
            player.displayClientMessage(Component.translatable("message.chinesedelight.cooking", percent), true);
            return InteractionResult.SUCCESS;
        }
        return pot.startCooking(level, pos).result();
    }
}
