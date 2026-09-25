package com.qiyi.chinesedelight.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import com.qiyi.chinesedelight.ChineseDelight;

public class ModTags {
    public static class Items {
        /** Items the iron pot will be able to process once cooking is implemented. */
        public static final TagKey<Item> TRANSFORMABLE_ITEMS = createTag("transformable_items");

        private static TagKey<Item> createTag(String name) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(ChineseDelight.MODID, name));
        }
    }
}
