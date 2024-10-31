package com.github.nalamodikk.common.recipe;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.recipe.fuel.FuelRecipe;
import com.github.nalamodikk.common.recipe.fuel.FuelRecipeBuilder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    // 註冊 RecipeSerializer 和 RecipeType
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, MagicalIndustryMod.MOD_ID);

    public static final DeferredRegister<RecipeType<?>> TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, MagicalIndustryMod.MOD_ID);

    // 統一使用 mana_crafting 名稱
    public static final RegistryObject<RecipeSerializer<ManaCraftingTableRecipe>> MANA_CRAFTING_SERIALIZER =
            SERIALIZERS.register("mana_crafting", ManaCraftingTableRecipe.Serializer::new);

    public static final RegistryObject<RecipeType<ManaCraftingTableRecipe>> MANA_CRAFTING_TYPE =
            TYPES.register("mana_crafting", () -> ManaCraftingTableRecipe.Type.INSTANCE);

    public static final RegistryObject<RecipeSerializer<FuelRecipe>> FUEL_RECIPE_SERIALIZER =
            SERIALIZERS.register("mana_fuel", () -> FuelRecipe.FuelRecipeSerializer.INSTANCE);

    public static final RegistryObject<RecipeType<FuelRecipe>> FUEL_RECIPE_TYPE =
            TYPES.register("mana_fuel", () -> FuelRecipe.FuelRecipeType.INSTANCE);

    // 註冊方法
    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
        TYPES.register(eventBus);
    }
}
