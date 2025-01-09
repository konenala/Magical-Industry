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
        registerConduit(ModBlocks.MANA_CONDUIT, "mana_conduit", "mana_conduit_texture");




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


    // 通用方法，為導管生成 BlockState 和物品模型
    private void registerConduit(RegistryObject<Block> blockRegistryObject, String conduitName, String textureName) {
        // 使用指定材質創建中心模型
        ModelFile centerModel = models().withExistingParent(conduitName, mcLoc("block/cube_all"))
                .texture("all", modLoc("block/conduit/" + textureName));

        // 初始化 Multipart Builder 並添加中心模型
        var multipartBuilder = getMultipartBuilder(blockRegistryObject.get());
        multipartBuilder.part().modelFile(centerModel).addModel().end();

        // 定義所有方向及其屬性對應關係
        Map<String, BooleanProperty> directions = Map.of(
                "north", ManaConduitBlock.NORTH,
                "east", ManaConduitBlock.EAST,
                "south", ManaConduitBlock.SOUTH,
                "west", ManaConduitBlock.WEST,
                "up", ManaConduitBlock.UP,
                "down", ManaConduitBlock.DOWN
        );

        // 對每個方向動態添加部分
        directions.forEach((directionName, property) ->
                multipartBuilder.part()
                        .modelFile(centerModel) // 使用相同的中心模型
                        .addModel()
                        .condition(property, true)
                        .end()
        );

        // 為物品生成模型
        itemModels().withExistingParent(conduitName, mcLoc("block/cube_all"))
                .texture("all", modLoc("block/conduit/" + textureName));
    }


}
