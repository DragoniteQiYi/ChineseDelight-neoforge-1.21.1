package com.qiyi.chinesedelight.cooking;

import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import com.qiyi.chinesedelight.ChineseDelight;

/** 中华乐事 (Chinese Delight) 的数据包注册表：食材数值、菜品、石磨配方、陶缸配方。全部可以在数据包里改。 */
public final class ModCookingRegistries {
    public static final ResourceKey<Registry<IngredientFlavor>> INGREDIENT = create("ingredient");
    public static final ResourceKey<Registry<Dish>> DISH = create("dish");
    public static final ResourceKey<Registry<GrindingRecipe>> GRINDING = create("grinding");
    public static final ResourceKey<Registry<FermentingRecipe>> FERMENTING = create("fermenting");

    private ModCookingRegistries() {
    }

    private static <T> ResourceKey<Registry<T>> create(String name) {
        return ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ChineseDelight.MODID, name));
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModCookingRegistries::registerRegistries);
    }

    private static void registerRegistries(DataPackRegistryEvent.NewRegistry event) {
        // 第三个参数是网络同步用的 codec：传了就会同步到客户端（厨师帽 HUD 要在客户端算菜品）
        event.dataPackRegistry(INGREDIENT, IngredientFlavor.CODEC, IngredientFlavor.CODEC);
        event.dataPackRegistry(DISH, Dish.CODEC, Dish.CODEC);
        event.dataPackRegistry(GRINDING, GrindingRecipe.CODEC, GrindingRecipe.CODEC);
        event.dataPackRegistry(FERMENTING, FermentingRecipe.CODEC, FermentingRecipe.CODEC);
    }

    // 1.21.1 的 RegistryAccess 只有 registryOrThrow，lookupOrThrow 是 1.21.2+ 才有的。
    public static Registry<IngredientFlavor> ingredients(RegistryAccess access) {
        return access.registryOrThrow(INGREDIENT);
    }

    public static Registry<Dish> dishes(RegistryAccess access) {
        return access.registryOrThrow(DISH);
    }

    public static Registry<GrindingRecipe> grinding(RegistryAccess access) {
        return access.registryOrThrow(GRINDING);
    }

    public static Registry<FermentingRecipe> fermenting(RegistryAccess access) {
        return access.registryOrThrow(FERMENTING);
    }
}
