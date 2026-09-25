package com.qiyi.chinesedelight.cooking;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Set;

/**
 * 客户端侧缓存：玩家已经做出来过的菜品（由服务端通过数据包同步）。
 * 放在 common 里是为了让数据包处理器不用引用客户端类。
 */
public final class CookingClientState {
    private static volatile Set<ResourceLocation> discovered = Set.of();

    private CookingClientState() {
    }

    public static void setDiscovered(Collection<ResourceLocation> dishes) {
        discovered = Set.copyOf(dishes);
    }

    public static boolean isDiscovered(ResourceLocation dish) {
        return discovered.contains(dish);
    }

    public static Set<ResourceLocation> getDiscovered() {
        return discovered;
    }
}
