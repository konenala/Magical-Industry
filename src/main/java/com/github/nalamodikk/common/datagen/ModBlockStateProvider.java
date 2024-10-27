package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.ModBlocks;
import com.github.nalamodikk.common.item.ModItems;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
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






        getVariantBuilder(ModBlocks.MANA_GENERATOR.get())
                .forAllStates(state -> ConfiguredModel.builder()
                        .modelFile(models().getExistingFile(modLoc("block/mana_generator")))
                        .rotationY(((int) state.getValue(BlockStateProperties.HORIZONTAL_FACING).toYRot()) % 360)
                        .build());

        blockWithItem(ModBlocks.DEEPSLATE_MAGIC_ORE);


        simpleBlockWithItem(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/mana_crafting_table")));

       /* simpleBlockWithItem(Block.byItem(ModItems.MANA_GENERATOR_BLOCK_ITEM.get()),
                new ModelFile.UncheckedModelFile(modLoc("block/mana_generator")));


        */

     /*   simpleBlockWithItem(ModBlocks.MANA_GENERATOR.get(),
                new ModelFile.UncheckedModelFile(modLoc("geo/mana_generator")));

      */

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
