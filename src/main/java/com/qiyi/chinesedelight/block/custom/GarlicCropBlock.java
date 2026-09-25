package com.qiyi.chinesedelight.block.custom;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.qiyi.chinesedelight.item.ModItems;

/**
 * 蒜：四个生长阶段，用蒜瓣种植。
 * <ul>
 *     <li>age=0 刚种下，打掉收回蒜瓣</li>
 *     <li>age=1 打掉收获蒜苗</li>
 *     <li>age=2 打掉收获蒜苗；此时右键可以采一次蒜薹（不破坏作物，靠 {@link #SCAPE_TAKEN} 记录是否已采过）</li>
 *     <li>age=3 打掉收获蒜头（外加蒜瓣）</li>
 * </ul>
 */
public class GarlicCropBlock extends CropBlock {
    public static final MapCodec<GarlicCropBlock> CODEC = simpleCodec(GarlicCropBlock::new);
    public static final int MAX_AGE = 3;
    /** 可以采蒜薹的阶段。 */
    public static final int SCAPE_AGE = 2;
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    /** 本阶段是否已经采过蒜薹，保证只能采一次。 */
    public static final BooleanProperty SCAPE_TAKEN = BooleanProperty.create("scape_taken");
    // 1.21.1 没有 Block#boxes/Block#column，按阶段手写一张表。
    private static final VoxelShape[] SHAPES = createShapes();

    private static VoxelShape[] createShapes() {
        VoxelShape[] shapes = new VoxelShape[MAX_AGE + 1];
        for (int age = 0; age <= MAX_AGE; age++) {
            shapes[age] = Block.box(0.0, 0.0, 0.0, 16.0, 3.0 + age * 4.0, 16.0);
        }
        return shapes;
    }

    public GarlicCropBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(SCAPE_TAKEN, false));
    }

    @Override
    public MapCodec<GarlicCropBlock> codec() {
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
        return ModItems.GARLIC_CLOVE;
    }

    /** 蒜薹是否还能采。 */
    public boolean isScapeReady(BlockState state) {
        return state.getValue(AGE) == SCAPE_AGE && !state.getValue(SCAPE_TAKEN);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        // 采蒜薹不能挡住骨粉催熟
        if (!this.isScapeReady(state) && stack.is(Items.BONE_MEAL)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!this.isScapeReady(state)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }

        if (level instanceof ServerLevel serverLevel) {
            popResource(serverLevel, pos, new ItemStack(ModItems.GARLIC_SCAPE.get()));
            serverLevel.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS,
                    1.0F, 0.8F + level.random.nextFloat() * 0.4F);
            BlockState picked = state.setValue(SCAPE_TAKEN, true);
            serverLevel.setBlock(pos, picked, 2);
            serverLevel.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        }

        return InteractionResult.SUCCESS;
    }

    /** 骨粉一次只催熟一个阶段（原版默认一次推进 2~5 个阶段）。 */
    @Override
    protected int getBonemealAgeIncrease(Level level) {
        return 1;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, SCAPE_TAKEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[this.getAge(state)];
    }
}
