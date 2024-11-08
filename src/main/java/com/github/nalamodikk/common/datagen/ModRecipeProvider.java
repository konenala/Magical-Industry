package com.github.nalamodikk.common.datagen;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.ModBlocks;
import com.github.nalamodikk.common.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
   /* private static final List<ItemLike> SAPPHIRE_SMELTABLES = List.of(ModItems.MANA_DUST.get(),
            ModBlocks.SAPPHIRE_ORE.get(), ModBlocks.DEEPSLATE_SAPPHIRE_ORE.get(), ModBlocks.NETHER_SAPPHIRE_ORE.get(),
            ModBlocks.END_STONE_SAPPHIRE_ORE.get());
    */



    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        // oreSmelting(pWriter, SAPPHIRE_SMELTABLES, RecipeCategory.MISC, ModItems.SAPPHIRE.get(), 0.25f, 200, "sapphire");
        // oreBlasting(pWriter, SAPPHIRE_SMELTABLES, RecipeCategory.MISC, ModItems.SAPPHIRE.get(), 0.25f, 100, "sapphire");
        FuelRecipeProvider.buildFuelRecipes(pWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_BLOCK.get())
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', ModItems.MANA_INGOT.get())
                .unlockedBy(getHasName(ModItems.MANA_INGOT.get()), has(ModItems.MANA_INGOT.get()))
                .save(pWriter);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.MANA_INGOT.get())
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S', ModItems.MANA_DUST.get())
                .unlockedBy(getHasName(ModItems.MANA_DUST.get()), has(ModItems.MANA_DUST.get()))
                .save(pWriter);


        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_INGOT.get(), 9)
                .requires(ModBlocks.MANA_BLOCK.get())
                .unlockedBy(getHasName(ModBlocks.MANA_BLOCK.get()), has(ModBlocks.MANA_BLOCK.get()))
                .save(pWriter, "mana_ingot_from_block");

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ModItems.MANA_DUST.get(), 9)
                .requires(ModItems.MANA_INGOT.get())
                .unlockedBy(getHasName(ModItems.MANA_INGOT.get()), has(ModItems.MANA_INGOT.get()))
                .save(pWriter,"mana_dust_from_ingot");


        // 魔力發電機配方
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.MANA_GENERATOR.get())
                .define('I', Items.IRON_INGOT)
                .define('R', Items.REDSTONE_BLOCK)
                .define('M', Items.DIAMOND_BLOCK)
                .define('F', Blocks.FURNACE)
                .define('A', Items.AMETHYST_SHARD)
                .pattern("AAA")
                .pattern("RMR")
                .pattern("IFI")
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pWriter);

        //魔力合成台
        ShapedRecipeBuilder.shaped(RecipeCategory.BUILDING_BLOCKS, ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get())
                .define('I', Items.IRON_INGOT)
                .define('D', Items.DIAMOND)
                .define('M', ModItems.BASIC_TECH_WAND.get() )
                .define('C', Items.CRAFTING_TABLE)
                .pattern("IDI")
                .pattern("IMI")
                .pattern("ICI")
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pWriter);

        //基礎科技法杖
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.BASIC_TECH_WAND.get())
                .define('I', ModItems.CORRUPTED_MANA_DUST.get())
                .define('R', Items.REDSTONE_BLOCK)
                .define('D', Blocks.DIAMOND_BLOCK)
                .define('C', Items.COPPER_INGOT)
                .define('A', Items.AMETHYST_SHARD)
                .pattern("RAR")
                .pattern("CIC")
                .pattern(" D ")
                .unlockedBy("has_iron", has(Items.IRON_INGOT))
                .save(pWriter);



    }

    protected static void oreSmelting(Consumer<FinishedRecipe> pFinishedRecipeConsumer, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(pFinishedRecipeConsumer, RecipeSerializer.SMELTING_RECIPE, pIngredients, pCategory, pResult, pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(Consumer<FinishedRecipe> pFinishedRecipeConsumer, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pFinishedRecipeConsumer, RecipeSerializer.BLASTING_RECIPE, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static void oreCooking(Consumer<FinishedRecipe> pFinishedRecipeConsumer, RecipeSerializer<? extends AbstractCookingRecipe> pCookingSerializer, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(itemlike), pCategory, pResult,
                    pExperience, pCookingTime, pCookingSerializer)
                    .group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(pFinishedRecipeConsumer,  MagicalIndustryMod.MOD_ID + ":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }
    }
}
