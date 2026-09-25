package com.qiyi.chinesedelight.cooking;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import com.qiyi.chinesedelight.ChineseDelight;

/**
 * 缓存从数据包注册表算出来的查表。
 * 标签内容会变（{@link TagsUpdatedEvent}），世界重载后 {@link RegistryAccess} 也是新对象，
 * 所以两边都做一次检查，保证查表不会过期。
 */
@EventBusSubscriber(modid = ChineseDelight.MODID)
public final class CookingDataCache {
    private static volatile RegistryAccess cachedAccess;
    private static volatile FlavorLookup ingredients = FlavorLookup.EMPTY;
    private static volatile Registry<Dish> dishes;
    private static volatile Registry<GrindingRecipe> grinding;
    private static volatile Registry<FermentingRecipe> fermenting;

    private CookingDataCache() {
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        // 1.21.1 的 TagsUpdatedEvent 直接给出 RegistryAccess（1.21.5 叫 getLookupProvider）。
        RegistryAccess access = event.getRegistryAccess();
        if (access != null) {
            rebuild(access);
        }
    }

    private static synchronized void rebuild(RegistryAccess access) {
        ingredients = FlavorLookup.build(ModCookingRegistries.ingredients(access));
        dishes = ModCookingRegistries.dishes(access);
        grinding = ModCookingRegistries.grinding(access);
        fermenting = ModCookingRegistries.fermenting(access);
        cachedAccess = access;
    }

    private static void ensure(RegistryAccess access) {
        if (access != cachedAccess || ingredients.isEmpty()) {
            rebuild(access);
        }
    }

    public static FlavorLookup ingredients(RegistryAccess access) {
        ensure(access);
        return ingredients;
    }

    public static Registry<Dish> dishes(RegistryAccess access) {
        ensure(access);
        return dishes;
    }

    public static Registry<GrindingRecipe> grinding(RegistryAccess access) {
        ensure(access);
        return grinding;
    }

    public static Registry<FermentingRecipe> fermenting(RegistryAccess access) {
        ensure(access);
        return fermenting;
    }
}
