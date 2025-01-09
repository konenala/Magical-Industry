package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.block.Conduit.ManaConduitBlock;
import com.github.nalamodikk.common.register.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

import java.util.Map;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, MagicalIndustryMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        blockWithItem(ModBlocks.MANA_BLOCK);
        blockWithItem(ModBlocks.MAGIC_ORE);
        // conduit 導管
        registerConduit(ModBlocks.MANA_CONDUIT, "mana_conduit");




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


    // conduit 魔力導管
    private void registerConduit(RegistryObject<Block> blockRegistryObject, String conduitName) {
        getMultipartBuilder(blockRegistryObject.get())
                .part().modelFile(models().getExistingFile(new ResourceLocation("magical_industry", "block/conduit/" + conduitName + "_center"))).addModel()
                .end();

        // 定義所有方向及其屬性對應關係
        Map<String, BooleanProperty> directions = Map.of(
                "north", ManaConduitBlock.NORTH,
                "east", ManaConduitBlock.EAST,
                "south", ManaConduitBlock.SOUTH,
                "west", ManaConduitBlock.WEST,
                "up", ManaConduitBlock.UP,
                "down", ManaConduitBlock.DOWN
        );

        // 動態處理每個方向
        for (Map.Entry<String, BooleanProperty> entry : directions.entrySet()) {
            String directionName = entry.getKey();
            BooleanProperty property = entry.getValue();

            getMultipartBuilder(blockRegistryObject.get())
                    .part()
                    .modelFile(models().getExistingFile(new ResourceLocation("magical_industry", "block/conduit/" + conduitName + "_" + directionName)))
                    .addModel()
                    .condition(property, true)
                    .end();
        }
    }

}
