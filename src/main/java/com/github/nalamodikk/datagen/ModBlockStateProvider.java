package com.github.nalamodikk.datagen;

import com.github.nalamodikk.MagicalIndustryMod;
import com.github.nalamodikk.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, MagicalIndustryMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.MANA_BLOCK);
        blockWithItem(ModBlocks.MAGIC_ORE);



        blockWithItem(ModBlocks.DEEPSLATE_MAGIC_ORE);


        simpleBlockWithItem(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/mana_crafting_table")));

        simpleBlockWithItem(ModBlocks.ADVANCED_MANA_CRAFTING_TABLE_BLOCK.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/mana_crafting_table")));

        /*
        blockWithItem(ModBlocks.SAPPHIRE_ORE);
        blockWithItem(ModBlocks.END_STONE_SAPPHIRE_ORE);
        blockWithItem(ModBlocks.NETHER_SAPPHIRE_ORE);

         */

    }

    private void blockWithItem(RegistryObject<Block> blockRegistryObject) {
        simpleBlockWithItem(blockRegistryObject.get(), cubeAll(blockRegistryObject.get()));
    }
}
