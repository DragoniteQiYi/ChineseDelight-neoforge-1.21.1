package com.qiyi.chinesedelight.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.qiyi.chinesedelight.ChineseDelight;
import com.qiyi.chinesedelight.block.ModBlocks;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ChineseDelight.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FLAVORS_TAB =
            CREATIVE_MODE_TABS.register("flavors_united", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.chinesedelight"))
                    .icon(() -> new ItemStack(ModBlocks.IRON_POT.get()))
                    .displayItems((parameters, output) -> {
                        // 调料
                        output.accept(ModItems.COOKING_OIL.get());
                        output.accept(ModItems.COOKING_SALT.get());
                        output.accept(ModItems.SOY_SAUCE.get());
                        output.accept(ModItems.VINEGAR.get());
                        // 食材
                        output.accept(ModItems.DUCK_EGG.get());
                        output.accept(ModItems.SALTED_DUCK_EGG.get());
                        output.accept(ModItems.OMELET.get());
                        output.accept(ModItems.RICE_PORRIDGE.get());
                        // 农作物
                        output.accept(ModItems.SCALLION_SEED.get());
                        output.accept(ModItems.SMALL_SCALLION.get());
                        output.accept(ModItems.BIG_SCALLION.get());
                        output.accept(ModItems.GINGER.get());
                        output.accept(ModItems.GARLIC_CLOVE.get());
                        output.accept(ModItems.GARLIC_SPROUT.get());
                        output.accept(ModItems.GARLIC_SCAPE.get());
                        output.accept(ModItems.GARLIC.get());
                        output.accept(ModItems.NAPA_CABBAGE_SEED.get());
                        output.accept(ModItems.NAPA_CABBAGE.get());
                        output.accept(ModItems.SOYBEAN.get());
                        output.accept(ModItems.EDAMAME.get());
                        // 饮品
                        output.accept(ModItems.SOYMILK_BUCKET.get());
                        output.accept(ModItems.COOKED_SOYMILK_BUCKET.get());
                        output.accept(ModItems.YOGURT_BUCKET.get());
                        // 加工原料
                        output.accept(ModItems.CHILI_SEED.get());
                        output.accept(ModItems.CHILI.get());
                        output.accept(ModItems.CHILI_POWDER.get());
                        output.accept(ModItems.PEPPERCORN_SEED.get());
                        output.accept(ModItems.PEPPERCORN.get());
                        output.accept(ModItems.FLOUR.get());
                        output.accept(ModItems.STARCH.get());
                        output.accept(ModItems.DOUGH.get());
                        // 加工成品
                        output.accept(ModItems.TOFU.get());
                        output.accept(ModItems.DRIED_TOFU.get());
                        output.accept(ModItems.DOUBANJIANG.get());
                        output.accept(ModItems.PICKLED_CABBAGE.get());
                        output.accept(ModItems.NOODLES.get());
                        output.accept(ModItems.STEAMED_BUN.get());
                        // 菜品（表在 ModDishItems 里，加菜会自动出现在这里）
                        for (ModDishItems.DishFood dish : ModDishItems.ALL) {
                            output.accept(ModDishItems.get(dish.id()).get());
                        }
                        // 厨具与方块
                        output.accept(ModItems.CHEF_HAT.get());
                        output.accept(ModItems.IRON_KITCHEN_KNIFE.get());
                        output.accept(ModBlocks.SALT_ORE.get());
                        output.accept(ModBlocks.IRON_PAN.get());
                        output.accept(ModBlocks.IRON_POT.get());
                        output.accept(ModBlocks.STONE_MILL.get());
                        output.accept(ModBlocks.FERMENTING_JAR.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
