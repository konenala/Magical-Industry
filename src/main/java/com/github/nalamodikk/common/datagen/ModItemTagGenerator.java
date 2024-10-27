package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagGenerator extends ItemTagsProvider {
    public ModItemTagGenerator(PackOutput p_275343_, CompletableFuture<HolderLookup.Provider> p_275729_,
                               CompletableFuture<TagLookup<Block>> p_275322_, @Nullable ExistingFileHelper existingFileHelper) {
        super(p_275343_, p_275729_, p_275322_, MagicalIndustryMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider pProvider) {

            // 使用 pProvider 作為標籤的來源
            tag(ItemTags.create(new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana")))
                    .add(ModItems.CORRUPTED_MANA_DUST.get()); // 將物品加入到 "mana" 標籤中
        }
    }

