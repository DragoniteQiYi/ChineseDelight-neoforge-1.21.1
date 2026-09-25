package com.qiyi.chinesedelight.block.custom;

import net.minecraft.world.ItemInteractionResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
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
import com.qiyi.chinesedelight.cooking.GrindingRecipe;
import com.qiyi.chinesedelight.entity.MillKnotEntity;
import com.qiyi.chinesedelight.entity.ModEntities;

import java.util.List;
import java.util.UUID;

/**
 * 石磨：不用 GUI，配方全部来自数据包注册表 {@code chinesedelight:grinding}。
 * <ul>
 *     <li>把能磨的东西丢在石磨上会被自动吸进去（也可以手持右键放进去）</li>
 *     <li>需要水的配方（例如豆浆）要先用桶加水</li>
 *     <li>空手右键一次推进 1 秒；攒够配方要求的时间就出一份</li>
 *     <li>{@code output_mode = bucket} 的产物（豆浆）要用空桶右键取；{@code item} 的产物（面粉）空手右键拿</li>
 *     <li>牵着牛/马右键可以把动物拴上，动物会绕着石磨转圈自动磨</li>
 *     <li>潜行 + 空手右键解开动物</li>
 * </ul>
 */
public class StoneMillBlockEntity extends BlockEntity {
    /** 手动右键一次推进的进度：1 秒。 */
    public static final int TICKS_PER_MANUAL_USE = 20;
    public static final int MAX_WATER = 8;
    public static final int MAX_INPUT = 64;
    public static final int MAX_OUTPUT = 64;
    public static final int MAX_PORTIONS = 8;

    private static final double ANIMAL_RADIUS = 1.5;
    private static final double ANIMAL_MAX_DISTANCE = 6.0;
    private static final float ANIMAL_ANGULAR_STEP = 0.03F;

    private ItemStack input = ItemStack.EMPTY;
    private ItemStack itemOutput = ItemStack.EMPTY;
    private ItemStack portionItem = ItemStack.EMPTY;
    private int portions;
    private int water;
    private int progress;
    private float animalAngle;
    @Nullable
    private UUID animalUuid;
    @Nullable
    private UUID knotUuid;

    public StoneMillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.STONE_MILL.get(), pos, state);
    }

    /* ------------------------------------------------------------------ 只读状态 */

    public ItemStack getInput() {
        return this.input;
    }

    public ItemStack getItemOutput() {
        return this.itemOutput;
    }

    public ItemStack getPortionItem() {
        return this.portionItem;
    }

    public int getPortions() {
        return this.portions;
    }

    public int getWater() {
        return this.water;
    }

    public int getProgress() {
        return this.progress;
    }

    public boolean hasBoundAnimal() {
        return this.animalUuid != null;
    }

    /* ------------------------------------------------------------------ 存档与同步 */

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveStack(tag, "input", this.input, registries);
        saveStack(tag, "item_output", this.itemOutput, registries);
        saveStack(tag, "portion_item", this.portionItem, registries);
        tag.putInt("portions", this.portions);
        tag.putInt("water", this.water);
        tag.putInt("progress", this.progress);
        if (this.animalUuid != null) {
            tag.put("animal", NbtUtils.createUUID(this.animalUuid));
        }
        if (this.knotUuid != null) {
            tag.put("knot", NbtUtils.createUUID(this.knotUuid));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.input = loadStack(tag, "input", registries);
        this.itemOutput = loadStack(tag, "item_output", registries);
        this.portionItem = loadStack(tag, "portion_item", registries);
        // 1.21.1 的 CompoundTag 没有 getIntOr/read，用 getInt（缺键返回 0）+ NbtUtils 读 UUID。
        this.portions = tag.getInt("portions");
        this.water = tag.getInt("water");
        this.progress = tag.getInt("progress");
        this.animalUuid = tag.contains("animal") ? NbtUtils.loadUUID(tag.get("animal")) : null;
        this.knotUuid = tag.contains("knot") ? NbtUtils.loadUUID(tag.get("knot")) : null;
    }

    private static void saveStack(CompoundTag tag, String key, ItemStack stack, HolderLookup.Provider registries) {
        if (!stack.isEmpty()) {
            tag.put(key, stack.save(registries));
        }
    }

    private static ItemStack loadStack(CompoundTag tag, String key, HolderLookup.Provider registries) {
        // 1.21.1 用 ItemStack.parseOptional(RegistryOps, CompoundTag)，没有 1.21.5 的 ItemStack.parse。
        return ItemStack.parseOptional(registries, tag.getCompound(key));
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

    public static void serverTick(Level level, BlockPos pos, BlockState state, StoneMillBlockEntity mill) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        mill.absorbItems(server, pos);
        boolean animalWorking = mill.tickBoundAnimal(server, pos);
        mill.grindTick(server, pos, animalWorking);
    }

    /** 当前输入对应的配方。 */
    @Nullable
    private GrindingRecipe currentRecipe(ServerLevel level) {
        if (this.input.isEmpty()) {
            return null;
        }
        for (GrindingRecipe recipe : CookingDataCache.grinding(level.registryAccess())) {
            if (recipe.matches(this.input)) {
                return recipe;
            }
        }
        return null;
    }

    /** 把丢在石磨上、并且有配方的东西吸进去。 */
    private void absorbItems(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 10L != 0L) {
            return;
        }
        if (!this.input.isEmpty() && this.input.getCount() >= MAX_INPUT) {
            return;
        }
        AABB area = new AABB(pos).inflate(0.75, 1.0, 0.75);
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, area)) {
            ItemStack stack = entity.getItem();
            if (stack.isEmpty() || entity.hasPickUpDelay() || !hasRecipeFor(level, stack)) {
                continue;
            }
            if (!this.input.isEmpty() && !ItemStack.isSameItemSameComponents(this.input, stack)) {
                continue;
            }
            int space = MAX_INPUT - this.input.getCount();
            if (space <= 0) {
                break;
            }
            int moved = Math.min(space, stack.getCount());
            if (this.input.isEmpty()) {
                this.input = stack.split(moved);
            } else {
                this.input.grow(moved);
                stack.shrink(moved);
            }
            if (stack.isEmpty()) {
                entity.discard();
            }
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.4F, 1.3F);
            this.setChanged();
            this.sync(level, pos);
            if (this.input.getCount() >= MAX_INPUT) {
                break;
            }
        }
    }

    private boolean hasRecipeFor(ServerLevel level, ItemStack stack) {
        for (GrindingRecipe recipe : CookingDataCache.grinding(level.registryAccess())) {
            if (recipe.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    /** 结算研磨进度。 */
    private void grindTick(ServerLevel level, BlockPos pos, boolean animalWorking) {
        GrindingRecipe recipe = this.currentRecipe(level);
        boolean canWork = recipe != null && (!recipe.water() || this.water > 0);
        boolean changed = false;

        if (!canWork) {
            if (this.progress != 0) {
                this.progress = 0;
                changed = true;
            }
        } else {
            if (animalWorking) {
                this.progress = Math.min(recipe.time(), this.progress + 1);
            }
            if (this.progress >= recipe.time()) {
                this.progress = 0;
                this.input.shrink(1);
                if (this.input.isEmpty()) {
                    this.input = ItemStack.EMPTY;
                }
                if (recipe.water()) {
                    this.water = Math.max(0, this.water - 1);
                }
                if (recipe.usesBucket()) {
                    this.portionItem = recipe.result().copy();
                    this.portions = Math.min(MAX_PORTIONS, this.portions + 1);
                } else {
                    if (this.itemOutput.isEmpty()) {
                        this.itemOutput = recipe.result().copy();
                    } else if (ItemStack.isSameItemSameComponents(this.itemOutput, recipe.result())) {
                        this.itemOutput.grow(recipe.result().getCount());
                    }
                    if (this.itemOutput.getCount() > MAX_OUTPUT) {
                        this.itemOutput.setCount(MAX_OUTPUT);
                    }
                }
                changed = true;
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.5F, 0.7F);
            }
        }

        boolean grinding = canWork && (animalWorking || this.progress > 0);
        BlockState state = this.getBlockState();
        if (state.getValue(StoneMillBlock.GRINDING) != grinding) {
            level.setBlock(pos, state.setValue(StoneMillBlock.GRINDING, grinding), Block.UPDATE_CLIENTS);
        }
        if (grinding && level.getGameTime() % 20L == 0L) {
            level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.5F,
                    0.7F + level.random.nextFloat() * 0.25F);
        }
        if (changed) {
            this.setChanged();
            this.sync(level, pos);
        }
    }

    /* ------------------------------------------------------------------ 动物驱动 */

    private static boolean isEligibleAnimal(Entity entity) {
        return entity instanceof Cow || entity instanceof AbstractHorse;
    }

    /** @return 被拴住的牛/马是否正在绕磨，也就是是否在自动研磨 */
    private boolean tickBoundAnimal(ServerLevel level, BlockPos pos) {
        if (this.animalUuid == null) {
            return false;
        }
        Entity entity = level.getEntities().get(this.animalUuid);
        if (!(entity instanceof Mob mob) || !entity.isAlive() || !isEligibleAnimal(entity)) {
            this.clearAnimalBinding(level, pos, false);
            return false;
        }

        MillKnotEntity knot = this.getOrCreateKnot(level, pos);
        if (knot == null) {
            return false;
        }
        Leashable leashable = (Leashable) entity;
        if (leashable.getLeashHolder() != knot) {
            Entity holder = leashable.getLeashHolder();
            if (holder == null || holder.getUUID().equals(this.knotUuid)) {
                leashable.setLeashedTo(knot, true);
            } else {
                this.clearAnimalBinding(level, pos, false);
                return false;
            }
        }

        double centerX = pos.getX() + 0.5;
        double centerZ = pos.getZ() + 0.5;
        double dx = mob.getX() - centerX;
        double dz = mob.getZ() - centerZ;
        if (dx * dx + dz * dz > ANIMAL_MAX_DISTANCE * ANIMAL_MAX_DISTANCE) {
            return false;
        }

        this.animalAngle = (this.animalAngle + ANIMAL_ANGULAR_STEP) % (float) (Math.PI * 2.0);
        double targetX = centerX + Math.cos(this.animalAngle) * ANIMAL_RADIUS;
        double targetZ = centerZ + Math.sin(this.animalAngle) * ANIMAL_RADIUS;

        mob.getNavigation().stop();
        double moveX = targetX - mob.getX();
        double moveZ = targetZ - mob.getZ();
        double distanceSqr = moveX * moveX + moveZ * moveZ;
        if (distanceSqr > 1.0E-4) {
            double length = Math.sqrt(distanceSqr);
            mob.setDeltaMovement(moveX / length * 0.055, mob.getDeltaMovement().y, moveZ / length * 0.055);
            float yaw = (float) (Mth.atan2(moveZ, moveX) * 180.0 / Math.PI) - 90.0F;
            mob.setYRot(Mth.approachDegrees(mob.getYRot(), yaw, 10.0F));
            mob.yBodyRot = mob.getYRot();
            mob.setYHeadRot(mob.getYRot());
        }
        return true;
    }

    @Nullable
    private MillKnotEntity getOrCreateKnot(ServerLevel level, BlockPos pos) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        if (this.knotUuid != null) {
            Entity existing = level.getEntities().get(this.knotUuid);
            if (existing instanceof MillKnotEntity knot) {
                knot.setPos(x, y, z);
                return knot;
            }
        }
        if (this.animalUuid == null) {
            return null;
        }
        MillKnotEntity knot = new MillKnotEntity(ModEntities.MILL_KNOT.get(), level);
        knot.setPos(x, y, z);
        level.addFreshEntity(knot);
        this.knotUuid = knot.getUUID();
        this.setChanged();
        return knot;
    }

    /** 牵着牛/马右键石磨即可拴上。 */
    public boolean bindAnimal(Level level, BlockPos pos, Player player) {
        Leashable candidate = findLeashedAnimal(level, pos, player);
        if (candidate == null) {
            return false;
        }
        if (!(level instanceof ServerLevel server)) {
            return true;
        }
        Entity animal = (Entity) candidate;
        if (animal.getUUID().equals(this.animalUuid)) {
            return true;
        }
        this.clearAnimalBinding(server, pos, true);
        this.animalUuid = animal.getUUID();
        MillKnotEntity knot = this.getOrCreateKnot(server, pos);
        if (knot == null) {
            this.animalUuid = null;
            return false;
        }
        candidate.setLeashedTo(knot, true);
        server.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.setChanged();
        this.sync(server, pos);
        return true;
    }

    @Nullable
    private static Leashable findLeashedAnimal(Level level, BlockPos pos, Player player) {
        AABB area = new AABB(pos).inflate(7.0);
        List<Entity> candidates = level.getEntitiesOfClass(Entity.class, area, StoneMillBlockEntity::isEligibleAnimal);
        for (Entity entity : candidates) {
            if (entity instanceof Leashable leashable && leashable.isLeashed() && leashable.getLeashHolder() == player) {
                return leashable;
            }
        }
        return null;
    }

    /** 潜行 + 空手右键可以解开动物。 */
    public boolean releaseAnimal(Level level, BlockPos pos) {
        if (this.animalUuid == null) {
            return false;
        }
        if (level instanceof ServerLevel server) {
            this.clearAnimalBinding(server, pos, true);
            server.playSound(null, pos, SoundEvents.LEASH_KNOT_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    private void clearAnimalBinding(ServerLevel level, BlockPos pos, boolean dropLead) {
        boolean hadBinding = this.animalUuid != null || this.knotUuid != null;
        if (this.animalUuid != null) {
            Entity animal = level.getEntities().get(this.animalUuid);
            if (animal instanceof Leashable leashable) {
                Entity holder = leashable.getLeashHolder();
                if (holder != null && holder.getUUID().equals(this.knotUuid)) {
                    if (dropLead) {
                        // 1.21.1 的 Leashable#dropLeash 需要显式给出两个开关。
                        leashable.dropLeash(true, true);
                    } else {
                        leashable.setLeashData(null);
                    }
                }
            }
        }
        if (this.knotUuid != null) {
            Entity knot = level.getEntities().get(this.knotUuid);
            if (knot != null) {
                knot.discard();
            }
        }
        this.animalUuid = null;
        this.knotUuid = null;
        this.animalAngle = 0.0F;
        this.progress = 0;
        if (hadBinding) {
            this.setChanged();
            this.sync(level, pos);
        }
    }

    /** 1.21.1 还没有 {@code preRemoveSideEffects}，改用 {@link BlockEntity#setRemoved()}。 */
    @Override
    public void setRemoved() {
        if (this.level instanceof ServerLevel server) {
            this.clearAnimalBinding(server, this.getBlockPos(), true);
        }
        super.setRemoved();
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

    /** 手持原料右键放进石磨。 */
    public ItemInteractionResult insertByHand(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (!(level instanceof ServerLevel server) || !hasRecipeFor(server, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!this.input.isEmpty() && !ItemStack.isSameItemSameComponents(this.input, stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int space = MAX_INPUT - this.input.getCount();
        if (space <= 0) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        int moved = Math.min(space, stack.getCount());
        if (this.input.isEmpty()) {
            this.input = stack.split(moved);
        } else {
            this.input.grow(moved);
            stack.shrink(moved);
        }
        server.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.2F);
        this.setChanged();
        this.sync(server, pos);
        return ItemInteractionResult.SUCCESS;
    }

    /** 空手右键推进 1 秒；有成品时先把成品拿走。 */
    public ItemInteractionResult grindManually(Level level, BlockPos pos, Player player) {
        if (!this.itemOutput.isEmpty()) {
            if (level instanceof ServerLevel server) {
                player.getInventory().placeItemBackInInventory(this.itemOutput.copy());
                this.itemOutput = ItemStack.EMPTY;
                server.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6F, 1.0F);
                this.setChanged();
                this.sync(server, pos);
            }
            return ItemInteractionResult.SUCCESS;
        }
        GrindingRecipe recipe = level instanceof ServerLevel server ? this.currentRecipe(server) : null;
        if (recipe == null || (recipe.water() && this.water <= 0)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            this.progress = Math.min(recipe.time(), this.progress + TICKS_PER_MANUAL_USE);
            server.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.7F,
                    0.9F + level.random.nextFloat() * 0.2F);
            this.setChanged();
        }
        return ItemInteractionResult.SUCCESS;
    }

    /** 空桶右键取走一份需要装桶的产物（豆浆）。 */
    public ItemInteractionResult takePortion(Level level, BlockPos pos, Player player, InteractionHand hand, ItemStack stack) {
        if (this.portions <= 0 || this.portionItem.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            this.portions--;
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, this.portionItem.copy()));
            server.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            this.setChanged();
            this.sync(server, pos);
        }
        return ItemInteractionResult.SUCCESS;
    }
}
