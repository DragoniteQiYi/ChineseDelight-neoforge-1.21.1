package com.qiyi.chinesedelight.item.custom;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

/**
 * 种子/种植物品用的方块物品。
 *
 * <p>1.21.1 的 {@link BlockItem#getDescriptionId()} 返回的是<b>方块</b>的翻译键
 * （{@code block.chinesedelight.gingercrop} = “姜”），而 1.21.5 里这类物品靠
 * {@code Item.Properties#useItemDescriptionPrefix()} 用<b>物品</b>的翻译键。
 * 语言文件里种子和收获物的名字是分开的（{@code item.chinesedelight.ginger} = “姜”，
 * {@code item.chinesedelight.scallionseed} = “葱种子”），所以这里显式改回物品键，
 * 免得种子显示成“葱”、姜显示成作物名。
 */
public class SeedBlockItem extends BlockItem {

    public SeedBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public String getDescriptionId() {
        return this.getOrCreateDescriptionId();
    }
}
