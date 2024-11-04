package com.github.nalamodikk.common.compat.JEI;

import com.github.nalamodikk.common.MagicalIndustryMod;
import com.github.nalamodikk.common.block.ModBlocks;
import com.github.nalamodikk.common.item.ModItems;
import com.github.nalamodikk.common.recipe.ManaCraftingTableRecipe;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

public class ManaCraftingTableCategory implements IRecipeCategory<ManaCraftingTableRecipe> {

    public static final ResourceLocation UID = new ResourceLocation(MagicalIndustryMod.MOD_ID, "mana_crafting");
    public static final ResourceLocation TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_crafting_table_gui.png");
    public static final ResourceLocation MANA_BAR_TEXTURE = new ResourceLocation(MagicalIndustryMod.MOD_ID, "textures/gui/mana_bar_full.png");

    public static final RecipeType<ManaCraftingTableRecipe> MANA_CRAFTING_TYPE =
            new RecipeType<>(UID, ManaCraftingTableRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableStatic manaCostDrawable;

    public ManaCraftingTableCategory(IGuiHelper helper) {
        this.background = helper.createDrawable(TEXTURE, 25, 15, 120, 60);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.MANA_CRAFTING_TABLE_BLOCK.get()));
        // 使用完整的魔力条纹理
        this.manaCostDrawable = helper.createDrawable(MANA_BAR_TEXTURE, 0, 0, 16, 16);  // 使用整张魔力条纹理，指定区域大小
    }

    @Override
    public RecipeType<ManaCraftingTableRecipe> getRecipeType() {
        return MANA_CRAFTING_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.magical_industry.mana_crafting_table");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ManaCraftingTableRecipe recipe, IFocusGroup focuses) {
        // 获取配方中的输入成分列表
        NonNullList<Ingredient> ingredients = recipe.getIngredients();

        // 遍历配方中的每个输入成分，添加到 3x3 的合成槽位
        for (int i = 0; i < ingredients.size(); i++) {
            int xPos = 5 + (i % 3) * 18;  // 设置 x 轴位置
            int yPos = 2 + (i / 3) * 18;  // 设置 y 轴位置
            builder.addSlot(RecipeIngredientRole.INPUT, xPos, yPos).addIngredients(ingredients.get(i));
        }

        // 添加合成结果槽位
        builder.addSlot(RecipeIngredientRole.OUTPUT, 99, 20).addItemStack(recipe.getResultItem(null));

        // 添加魔力消耗渲染部分
        int manaCostX = 95;  // 将魔力消耗条渲染到左侧
        int manaCostY = 45;
        int manaCost = recipe.getManaCost();


        builder.addSlot(RecipeIngredientRole.CATALYST, manaCostX, manaCostY)
                .setBackground(this.manaCostDrawable, 0, 0)
                .addItemStack(new ItemStack(ModItems.MANA_DUST.get()))  // 使用一个象征魔力的物品
                .addTooltipCallback((recipeSlotView, tooltip) -> {
                    if (recipeSlotView.getRole() == RecipeIngredientRole.CATALYST) {
                        tooltip.add(Component.translatable("tooltip.magical_industry.mana_cost", recipe.getManaCost()));
                    }
                });

         }

    }
