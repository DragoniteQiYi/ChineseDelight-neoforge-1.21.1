package com.qiyi.chinesedelight.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import com.qiyi.chinesedelight.ChineseDelight;
import com.qiyi.chinesedelight.block.ModBlocks;
import com.qiyi.chinesedelight.item.custom.IronKitchenKnife;
import com.qiyi.chinesedelight.item.custom.SeedBlockItem;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(ChineseDelight.MODID);

    /** 厨师帽的盔甲材质。1.21.1 的盔甲走 {@link ArmorMaterial} 注册表，没有 1.21.5 的 equipment 组件。 */
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, ChineseDelight.MODID);

    /** 厨师帽贴图：assets/chinesedelight/textures/models/armor/chef_hat_layer_1.png。 */
    public static final ResourceLocation CHEF_HAT_ASSET =
            ResourceLocation.fromNamespaceAndPath(ChineseDelight.MODID, "chef_hat");

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> CHEF_HAT_MATERIAL =
            ARMOR_MATERIALS.register("chef_hat", () -> {
                Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
                for (ArmorItem.Type type : ArmorItem.Type.values()) {
                    defense.put(type, 0);
                }
                return new ArmorMaterial(
                        defense,
                        0,
                        SoundEvents.ARMOR_EQUIP_LEATHER,
                        () -> Ingredient.of(Items.LEATHER),
                        List.of(new ArmorMaterial.Layer(CHEF_HAT_ASSET)),
                        0.0F,
                        0.0F);
            });

    // 调料
    public static final DeferredItem<Item> COOKING_OIL = ITEMS.registerSimpleItem("cookingoil");

    public static final DeferredItem<Item> COOKING_SALT = ITEMS.registerSimpleItem("cookingsalt");

    public static final DeferredItem<Item> SOY_SAUCE = ITEMS.registerSimpleItem("soysauce");

    public static final DeferredItem<Item> VINEGAR = ITEMS.registerSimpleItem("vinegar");

    // 食材
    public static final DeferredItem<Item> DUCK_EGG = ITEMS.registerSimpleItem("duckegg");

    public static final DeferredItem<Item> SALTED_DUCK_EGG = ITEMS.registerSimpleItem("saltedduckegg");

    public static final DeferredItem<Item> OMELET = ITEMS.registerSimpleItem("omelet");

    public static final DeferredItem<Item> RICE_PORRIDGE = ITEMS.registerSimpleItem("riceporridge");

    // 厨具
    public static final DeferredItem<Item> IRON_KITCHEN_KNIFE = ITEMS.registerItem("ironkitchenknife", IronKitchenKnife::new);

    // 农作物：这几种物品既是种子也是食材，右键耕地就能种下。
    // 1.21.1 没有 Item.Properties#useItemDescriptionPrefix，物品说明前缀本来就不存在。
    public static final DeferredItem<BlockItem> SCALLION_SEED = ITEMS.registerItem("scallionseed",
            props -> new SeedBlockItem(ModBlocks.SCALLION_CROP.get(), props));

    public static final DeferredItem<BlockItem> GINGER = ITEMS.registerItem("ginger",
            props -> new SeedBlockItem(ModBlocks.GINGER_CROP.get(), props));

    public static final DeferredItem<BlockItem> GARLIC_CLOVE = ITEMS.registerItem("garlicclove",
            props -> new SeedBlockItem(ModBlocks.GARLIC_CROP.get(), props));

    // 收获物
    public static final DeferredItem<Item> SMALL_SCALLION = ITEMS.registerSimpleItem("smallscallion");

    public static final DeferredItem<Item> BIG_SCALLION = ITEMS.registerSimpleItem("bigscallion");

    public static final DeferredItem<Item> GARLIC_SPROUT = ITEMS.registerSimpleItem("garlicsprout");

    public static final DeferredItem<Item> GARLIC_SCAPE = ITEMS.registerSimpleItem("garlicscape");

    public static final DeferredItem<Item> GARLIC = ITEMS.registerSimpleItem("garlic");

    public static final DeferredItem<BlockItem> NAPA_CABBAGE_SEED = ITEMS.registerItem("napacabbageseed",
            props -> new SeedBlockItem(ModBlocks.NAPA_CABBAGE_CROP.get(), props));

    public static final DeferredItem<Item> NAPA_CABBAGE = ITEMS.registerSimpleItem("napacabbage");

    public static final DeferredItem<BlockItem> SOYBEAN = ITEMS.registerItem("soybean",
            props -> new SeedBlockItem(ModBlocks.SOYBEAN_CROP.get(), props));

    public static final DeferredItem<Item> EDAMAME = ITEMS.registerSimpleItem("edamame");

    // 饮品：生豆浆喝了恶心+中毒，熔炉烧 10 秒变成熟豆浆。
    // 1.21.1 没有 Item.Properties#usingConvertsTo：喝空后归还桶写在 FoodProperties 的 usingConvertsTo 里（见 ModFoods）。
    public static final DeferredItem<Item> SOYMILK_BUCKET = ITEMS.registerItem("soymilk_bucket", Item::new,
            new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .food(ModFoods.RAW_SOYMILK)
                    .stacksTo(1));

    public static final DeferredItem<Item> COOKED_SOYMILK_BUCKET = ITEMS.registerItem("cookedsoymilk_bucket", Item::new,
            new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .food(ModFoods.COOKED_SOYMILK)
                    .stacksTo(1));

    // ---------------------------------------------------------------- 加工链
    public static final DeferredItem<Item> FLOUR = ITEMS.registerSimpleItem("flour");

    public static final DeferredItem<Item> STARCH = ITEMS.registerSimpleItem("starch");

    public static final DeferredItem<Item> DOUGH = ITEMS.registerSimpleItem("dough");

    public static final DeferredItem<Item> CHILI_POWDER = ITEMS.registerSimpleItem("chilipowder");

    public static final DeferredItem<Item> PEPPERCORN = ITEMS.registerSimpleItem("peppercorn");

    public static final DeferredItem<BlockItem> PEPPERCORN_SEED = ITEMS.registerItem("peppercornseed",
            props -> new SeedBlockItem(ModBlocks.PEPPERCORN_CROP.get(), props));

    public static final DeferredItem<Item> RED_CHILLI = ITEMS.registerSimpleItem("red_chilli");

    public static final DeferredItem<BlockItem> CHILI_SEED = ITEMS.registerItem("chiliseed",
            props -> new SeedBlockItem(ModBlocks.CHILI_CROP.get(), props));

    public static final DeferredItem<Item> TOFU = ITEMS.registerItem("tofu", Item::new,
            new Item.Properties().food(ModFoods.TOFU));

    public static final DeferredItem<Item> DRIED_TOFU = ITEMS.registerItem("driedtofu", Item::new,
            new Item.Properties().food(ModFoods.DRIED_TOFU));

    public static final DeferredItem<Item> DOUBANJIANG = ITEMS.registerSimpleItem("doubanjiang");

    public static final DeferredItem<Item> PICKLED_CABBAGE = ITEMS.registerItem("pickledcabbage", Item::new,
            new Item.Properties().food(ModFoods.PICKLED_CABBAGE));

    public static final DeferredItem<Item> NOODLES = ITEMS.registerItem("noodles", Item::new,
            new Item.Properties().food(ModFoods.NOODLES));

    public static final DeferredItem<Item> STEAMED_BUN = ITEMS.registerItem("steamedbun", Item::new,
            new Item.Properties().food(ModFoods.STEAMED_BUN));

    public static final DeferredItem<Item> YOGURT_BUCKET = ITEMS.registerItem("yogurt_bucket", Item::new,
            new Item.Properties()
                    .craftRemainder(Items.BUCKET)
                    .food(ModFoods.YOGURT)
                    .stacksTo(1));

    // ---------------------------------------------------------------- 厨师帽
    /**
     * 戴上以后看铁锅会显示锅内详情和预计出锅的菜（吃过一次的菜才会显示名字）。
     * 1.21.1 用 {@link ArmorItem} + 自定义 {@link ArmorMaterial}，头部槽位材料防御为 0。
     */
    public static final DeferredItem<ArmorItem> CHEF_HAT = ITEMS.registerItem("chefhat",
            props -> new ArmorItem(CHEF_HAT_MATERIAL, ArmorItem.Type.HELMET, props.stacksTo(1)));

    // ---------------------------------------------------------------- 菜品
    static {
        ModDishItems.register(ITEMS);
    }

    public static void register(IEventBus eventBus){
        ARMOR_MATERIALS.register(eventBus);
        ITEMS.register(eventBus);
    }
}
