package com.qiyi.chinesedelight.block.custom;

import net.minecraft.world.ItemInteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import com.qiyi.chinesedelight.block.ModBlockEntities;
import com.qiyi.chinesedelight.cooking.CookingDataCache;
import com.qiyi.chinesedelight.cooking.CookingEvaluator;
import com.qiyi.chinesedelight.cooking.FlavorLookup;
import com.qiyi.chinesedelight.cooking.OutputHelper;
import com.qiyi.chinesedelight.network.ModNetwork;

import java.util.List;

/**
 * 铁锅：4 个格子，不用 GUI。
 * <ul>
 *     <li>食材/调料用 Q 丢进锅里（或丢在锅上面），一次吸一格</li>
 *     <li>手持水桶右键加水（每道菜消耗 1 份）</li>
 *     <li>4 格放满就自动开火；不足 4 格时空手右键手动开火</li>
 *     <li>潜行 + 空手右键把食材退出来</li>
 * </ul>
 */
public class IronPotBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    /** 一道菜要煮多久（6 秒）。 */
    public static final int COOK_TIME = 120;
    public static final int MAX_WATER = 4;
    public static final String STATION = "iron_pot";

    private final NonNullList<ItemStack> contents = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int water;
    private int cookProgress;
    private boolean cooking;

    public IronPotBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IRON_POT.get(), pos, state);
    }

    /* ------------------------------------------------------------------ 状态读取 */

    public NonNullList<ItemStack> getContents() {
        return this.contents;
    }

    public int getWater() {
        return this.water;
    }

    public int getCookProgress() {
        return this.cookProgress;
    }

    public boolean isCooking() {
        return this.cooking;
    }

    public boolean isEmpty() {
        return this.contents.stream().allMatch(ItemStack::isEmpty);
    }

    public List<ItemStack> contentList() {
        return List.copyOf(this.contents);
    }

    /* ------------------------------------------------------------------ 存档与同步 */

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.contents, registries);
        tag.putInt("water", this.water);
        tag.putInt("cook_progress", this.cookProgress);
        tag.putBoolean("cooking", this.cooking);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.contents.clear();
        ContainerHelper.loadAllItems(tag, this.contents, registries);
        // 1.21.1 的 CompoundTag 没有 getIntOr/getBooleanOr。
        this.water = tag.getInt("water");
        this.cookProgress = tag.getInt("cook_progress");
        this.cooking = tag.getBoolean("cooking");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync(ServerLevel level, BlockPos pos) {
        BlockState state = this.getBlockState();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_CLIENTS);
    }

    /* ------------------------------------------------------------------ 每刻逻辑 */

    public static void serverTick(Level level, BlockPos pos, BlockState state, IronPotBlockEntity pot) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        pot.absorbItems(server, pos);
        if (pot.cooking) {
            pot.cookProgress++;
            if (pot.cookProgress % 20 == 0) {
                server.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.5F,
                        0.9F + server.random.nextFloat() * 0.2F);
                server.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5,
                        1, 0.1, 0.05, 0.1, 0.01);
            }
            if (pot.cookProgress >= COOK_TIME) {
                pot.finishCooking(server, pos);
            }
        } else if (pot.isFull() && pot.water > 0) {
            pot.startCooking(server, pos);
        }
    }

    /** 把丢在锅上的食材一格一格吸进去。 */
    private void absorbItems(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 10L != 0L || this.isFull()) {
            return;
        }
        FlavorLookup lookup = CookingDataCache.ingredients(level.registryAccess());
        AABB area = new AABB(pos).inflate(0.75, 1.2, 0.75);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty() || entity.hasPickUpDelay()) {
                continue;
            }
            if (!lookup.isCookable(stack) || OutputHelper.isDishResult(level.registryAccess(), stack)) {
                continue;
            }
            int slot = this.firstEmptySlot();
            if (slot < 0) {
                break;
            }
            this.contents.set(slot, stack.split(1));
            if (stack.isEmpty()) {
                entity.discard();
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.3F);
            this.setChanged();
            this.sync(level, pos);
            if (this.isFull()) {
                break;
            }
        }
    }

    private boolean isFull() {
        return this.firstEmptySlot() < 0;
    }

    private int firstEmptySlot() {
        for (int i = 0; i < SLOTS; i++) {
            if (this.contents.get(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /** 出锅：判定菜品 → 掉出来 → 记一笔“新菜谱”。 */
    private void finishCooking(ServerLevel level, BlockPos pos) {
        CookingEvaluator.Result result = CookingEvaluator.evaluate(
                CookingDataCache.dishes(level.registryAccess()),
                CookingDataCache.ingredients(level.registryAccess()),
                this.contents, STATION);

        this.cooking = false;
        this.cookProgress = 0;
        this.water = Math.max(0, this.water - 1);

        if (result == null) {
            // 数据包里没有兜底料理时不吞食材，原样退回
            for (ItemStack stack : this.contents) {
                if (!stack.isEmpty()) {
                    OutputHelper.pop(level, pos, stack.copy());
                }
            }
        } else {
            ItemStack out = result.dish().result().copy();
            OutputHelper.pop(level, pos, out);
            ModNetwork.awardDish(level, pos, result.id(), out.getHoverName());
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6F, 1.2F);
        }

        clearContents();
        this.setChanged();
        this.sync(level, pos);
    }

    private void clearContents() {
        for (int i = 0; i < SLOTS; i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
    }

    /* ------------------------------------------------------------------ 交互 */

    public ItemInteractionResult addWater(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (this.water >= MAX_WATER) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            this.water++;
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            server.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.setChanged();
            this.sync(server, pos);
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** 空手右键：不足 4 格时手动开火。 */
    public ItemInteractionResult startCooking(Level level, BlockPos pos) {
        if (this.cooking || this.isEmpty() || this.water <= 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            startCooking(server, pos);
        }
        return ItemInteractionResult.SUCCESS;
    }

    private void startCooking(ServerLevel level, BlockPos pos) {
        this.cooking = true;
        this.cookProgress = 0;
        level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 0.6F, 1.4F);
        this.setChanged();
        this.sync(level, pos);
    }

    /** 潜行 + 空手右键：把食材退出来。 */
    public ItemInteractionResult ejectContents(Level level, BlockPos pos) {
        if (this.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            for (ItemStack stack : this.contents) {
                if (!stack.isEmpty()) {
                    Block.popResource(server, pos.above(), stack.copy());
                }
            }
            clearContents();
            this.cooking = false;
            this.cookProgress = 0;
            server.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 0.8F);
            this.setChanged();
            this.sync(server, pos);
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** 供 HUD / 调试用：按当前锅里的东西预测会出什么菜。 */
    @Nullable
    public CookingEvaluator.Result peekResult() {
        if (this.level == null || this.isEmpty()) {
            return null;
        }
        return CookingEvaluator.evaluate(
                CookingDataCache.dishes(this.level.registryAccess()),
                CookingDataCache.ingredients(this.level.registryAccess()),
                this.contents, STATION);
    }
}
