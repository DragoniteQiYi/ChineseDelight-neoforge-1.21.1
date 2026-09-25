package com.qiyi.chinesedelight.block.custom;

import net.minecraft.world.ItemInteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import com.qiyi.chinesedelight.cooking.FermentingRecipe;
import com.qiyi.chinesedelight.cooking.FlavorLookup;
import com.qiyi.chinesedelight.cooking.OutputHelper;

import java.util.List;

/**
 * 陶缸：丢进符合配方的食材后自动开始发酵 / 腌制 / 和面，时间到了把产物喷出来。
 */
public class FermentingJarBlockEntity extends BlockEntity {
    public static final int SLOTS = 4;
    public static final int MAX_WATER = 4;

    private final NonNullList<ItemStack> contents = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int water;
    private int progress;
    @Nullable
    private FermentingRecipe activeRecipe;

    public FermentingJarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FERMENTING_JAR.get(), pos, state);
    }

    /* ------------------------------------------------------------------ 状态读取 */

    public NonNullList<ItemStack> getContents() {
        return this.contents;
    }

    public int getWater() {
        return this.water;
    }

    public int getProgress() {
        return this.progress;
    }

    public boolean isWorking() {
        return this.activeRecipe != null;
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
        tag.putInt("progress", this.progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.contents.clear();
        ContainerHelper.loadAllItems(tag, this.contents, registries);
        // 1.21.1 的 CompoundTag 没有 getIntOr。
        this.water = tag.getInt("water");
        this.progress = tag.getInt("progress");
        this.activeRecipe = null;
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, FermentingJarBlockEntity jar) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        jar.absorbItems(server, pos);

        if (jar.isEmpty()) {
            if (jar.activeRecipe != null) {
                jar.reset();
                jar.setChanged();
                jar.sync(server, pos);
            }
            return;
        }

        FermentingRecipe recipe = jar.findRecipe(server);
        if (recipe == null) {
            if (jar.activeRecipe != null) {
                // 材料被换掉了，重新开始
                jar.reset();
                jar.setChanged();
                jar.sync(server, pos);
            }
            return;
        }

        if (jar.activeRecipe == null) {
            jar.activeRecipe = recipe;
            jar.progress = 0;
            jar.setChanged();
            jar.sync(server, pos);
        }

        jar.progress++;
        if (jar.progress % 40 == 0) {
            server.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    1, 0.15, 0.1, 0.15, 0.01);
        }
        if (jar.progress >= recipe.time()) {
            jar.finish(server, pos, recipe);
        }
    }

    private boolean isEmpty() {
        return this.contents.stream().allMatch(ItemStack::isEmpty);
    }

    @Nullable
    private FermentingRecipe findRecipe(ServerLevel level) {
        for (FermentingRecipe recipe : CookingDataCache.fermenting(level.registryAccess())) {
            if (recipe.water() && this.water <= 0) {
                continue;
            }
            if (recipe.matches(this.contents)) {
                return recipe;
            }
        }
        return null;
    }

    private void finish(ServerLevel level, BlockPos pos, FermentingRecipe recipe) {
        ItemStack out = recipe.result().copy();
        this.progress = 0;
        this.activeRecipe = null;
        if (recipe.water()) {
            this.water = Math.max(0, this.water - 1);
        }
        clearContents();
        OutputHelper.pop(level, pos, out);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.7F, 1.0F);
        this.setChanged();
        this.sync(level, pos);
    }

    private void reset() {
        this.progress = 0;
        this.activeRecipe = null;
    }

    private void clearContents() {
        for (int i = 0; i < SLOTS; i++) {
            this.contents.set(i, ItemStack.EMPTY);
        }
    }

    private void absorbItems(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        FlavorLookup lookup = CookingDataCache.ingredients(level.registryAccess());
        AABB area = new AABB(pos).inflate(0.75, 1.2, 0.75);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty() || entity.hasPickUpDelay() || !lookup.isCookable(stack)) {
                continue;
            }
            int slot = this.firstMergeableSlot(stack);
            if (slot < 0) {
                break;
            }
            ItemStack current = this.contents.get(slot);
            if (current.isEmpty()) {
                this.contents.set(slot, stack.split(Math.min(stack.getCount(), stack.getMaxStackSize())));
            } else {
                int moved = Math.min(stack.getCount(), current.getMaxStackSize() - current.getCount());
                if (moved <= 0) {
                    break;
                }
                current.grow(moved);
                stack.shrink(moved);
            }
            if (stack.isEmpty()) {
                entity.discard();
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.3F);
            this.setChanged();
            this.sync(level, pos);
        }
    }

    private int firstMergeableSlot(ItemStack stack) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack current = this.contents.get(i);
            if (current.isEmpty()) {
                return i;
            }
            if (ItemStack.isSameItemSameComponents(current, stack) && current.getCount() < current.getMaxStackSize()) {
                return i;
            }
        }
        return -1;
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
            reset();
            server.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 0.8F);
            this.setChanged();
            this.sync(server, pos);
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** 空手右键：把状态发到动作栏。 */
    public void showStatus(Player player) {
        if (this.activeRecipe == null) {
            player.displayClientMessage(Component.translatable("message.chinesedelight.jar_idle"), true);
            return;
        }
        int percent = Math.min(100, this.progress * 100 / Math.max(1, this.activeRecipe.time()));
        player.displayClientMessage(Component.translatable("message.chinesedelight.jar_progress", percent), true);
    }
}
