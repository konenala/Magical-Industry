package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, MagicalIndustryMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simpleItem(ModItems.MANA_DEBUG_TOOL);
        simpleItem(ModItems.BASIC_TECH_WAND);
        simpleItem(ModItems.MANA_DUST);
        simpleItem(ModItems.MANA_INGOT);
        simpleItem(ModItems.CORRUPTED_MANA_DUST);



    }

    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/generated")).texture("layer0",
                new ResourceLocation(MagicalIndustryMod.MOD_ID,"item/" + item.getId().getPath()));
    }
}
