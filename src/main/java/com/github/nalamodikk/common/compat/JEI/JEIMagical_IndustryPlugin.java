package com.github.nalamodikk.common.compat.JEI;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.ModBlocks;
import com.github.nalamodikk.common.recipe.ManaCraftingTableRecipe;
import com.github.nalamodikk.common.screen.manacrafting.ManaCraftingScreen;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.List;

@JeiPlugin
public class JEIMagical_IndustryPlugin implements IModPlugin {

    public static final ResourceLocation PLUGIN_UID = new ResourceLocation(MagicalIndustryMod.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        // 注册自定义配方类别 (魔力合成台)
        registration.addRecipeCategories(new ManaCraftingTableCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // 注册魔力合成台作为配方的催化剂 (即 JEI 中显示的合成台图标)
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get()),
                ManaCraftingTableCategory.MANA_CRAFTING_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // 获取当前的配方管理器
        RecipeManager recipeManager = Minecraft.getInstance().level.getRecipeManager();

        // 获取所有的魔力合成台配方
        List<ManaCraftingTableRecipe> manaCraftingTableRecipes =
                recipeManager.getAllRecipesFor(ManaCraftingTableRecipe.Type.INSTANCE);

        // 注册魔力合成台的配方到 JEI 中
        registration.addRecipes(ManaCraftingTableCategory.MANA_CRAFTING_TYPE, manaCraftingTableRecipes);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 注册 GUI 的点击区域，使玩家能够在 JEI 中点击魔力合成台的配方以查看
        registration.addRecipeClickArea(ManaCraftingScreen.class, 100, 40, 20, 30,
                ManaCraftingTableCategory.MANA_CRAFTING_TYPE);
    }
}
