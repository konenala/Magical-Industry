package com.github.nalamodikk.common.datagen;


import com.github.nalamodikk.common.recipe.fuel.FuelRecipeBuilder;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public class FuelRecipeProvider {

    public static void buildFuelRecipes(Consumer<FinishedRecipe> pWriter) {
        // 定義你的燃料配方
        FuelRecipeBuilder.create("magical_industry:corrupted_mana_dust", 200, 100, 300, "corrupted_mana_dust_fuel")
                .save(pWriter);

        FuelRecipeBuilder.create("magical_industry:mana_dust", 400, 100, 500, "mana_dust_fuel")
                .save(pWriter);

    }
}
