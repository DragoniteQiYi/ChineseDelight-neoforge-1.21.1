package com.qiyi.chinesedelight.cooking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;

/** 机器出料的小工具：喷出来的东西要有拾取延迟，否则会被机器自己重新吸回去。 */
public final class OutputHelper {
    /** 机器出料的拾取延迟（10 秒）。 */
    public static final int OUTPUT_PICKUP_DELAY = 200;

    private OutputHelper() {
    }

    public static void pop(ServerLevel level, BlockPos pos, ItemStack stack) {
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, stack);
        entity.setDeltaMovement((level.random.nextDouble() - 0.5) * 0.08, 0.22, (level.random.nextDouble() - 0.5) * 0.08);
        entity.setPickUpDelay(OUTPUT_PICKUP_DELAY);
        level.addFreshEntity(entity);
    }

    /** 这道菜是不是某道菜的产物（用来防止成品被当成食材再下锅）。 */
    public static boolean isDishResult(RegistryAccess access, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Registry<Dish> dishes = CookingDataCache.dishes(access);
        for (Dish dish : dishes) {
            if (ItemStack.isSameItem(dish.result(), stack)) {
                return true;
            }
        }
        return false;
    }
}
