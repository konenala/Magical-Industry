package com.github.nalamodikk.common.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ManaCraftingTableRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final NonNullList<Ingredient> inputItems;  // 支持多个输入物品
    private final ItemStack output;                    // 输出物品
    private final int manaCost;                        // 魔力消耗字段

    public ManaCraftingTableRecipe(ResourceLocation id, NonNullList<Ingredient> inputItems, ItemStack output, int manaCost) {
        this.id = id;
        this.inputItems = inputItems;
        this.output = output;
        this.manaCost = manaCost; // 初始化魔力消耗字段
    }

    @Override
    public boolean matches(SimpleContainer inv, Level world) {
        if (world.isClientSide()) {
            return false;
        }

        // 检查容器大小
        if (inv.getContainerSize() != 9) {
            return false;
        }

        List<Ingredient> remainingIngredients = new ArrayList<>(inputItems);
    //    System.out.println("Starting match test with input items: " + inputItems);

        for (int i = 0; i < 9; i++) {
            ItemStack stackInSlot = inv.getItem(i);
      //System.out.println("Checking slot " + i + ": " + stackInSlot);

            if (stackInSlot.isEmpty()) {
                continue;
            }

            boolean matched = false;

            Iterator<Ingredient> iterator = remainingIngredients.iterator();
            while (iterator.hasNext()) {
                Ingredient ingredient = iterator.next();
                if (ingredient.test(stackInSlot)) {
                //    System.out.println("Matched ingredient in slot " + i + ": " + stackInSlot);
                    iterator.remove();
                    matched = true;
                    break;
                }
            }

            if (!matched) {
               // System.out.println("No match found for slot " + i);
                return false;
            }
        }

        boolean allMatched = remainingIngredients.isEmpty();
        //System.out.println("Match result: " + allMatched);
        return allMatched;
    }

    public NonNullList<Ingredient> getInputItems() {
        return inputItems;
    }

    @Override
    public ItemStack assemble(SimpleContainer pContainer, RegistryAccess pRegistryAccess) {
        return output.copy();  // 返回合成结果
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= inputItems.size();  // 检查配方空间是否足够
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MANA_CRAFTING_SERIALIZER.get();  // 返回注册的序列化器
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.MANA_CRAFTING_TYPE.get();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inputItems;
    }

    // 新增获取魔力消耗的方法
    public int getManaCost() {
        return manaCost;
    }

    // 自定义 Type 类
    public static class Type implements RecipeType<ManaCraftingTableRecipe> {
        public static final Type INSTANCE = new Type();
        public static final String ID = "mana_crafting";  // 统一名称
    }

    // 序列化器类
    public static class Serializer implements RecipeSerializer<ManaCraftingTableRecipe> {
        public static final Serializer INSTANCE = new Serializer();

        // 从 JSON 数据读取配方
        @Override
        public ManaCraftingTableRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            JsonArray ingredients = json.getAsJsonArray("ingredients");
            NonNullList<Ingredient> inputItems = NonNullList.withSize(ingredients.size(), Ingredient.EMPTY);

            for (int i = 0; i < inputItems.size(); i++) {
                inputItems.set(i, Ingredient.fromJson(ingredients.get(i)));
            }

            JsonObject outputObj = json.getAsJsonObject("output");
            ItemStack output = new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(outputObj.get("item").getAsString())),
                    outputObj.get("count").getAsInt());

            int manaCost = json.get("manaCost").getAsInt();  // 读取魔力消耗值

            return new ManaCraftingTableRecipe(recipeId, inputItems, output, manaCost);
        }

        // 从网络缓冲区读取配方
        @Override
        public ManaCraftingTableRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            int size = buffer.readInt();
            NonNullList<Ingredient> inputItems = NonNullList.withSize(size, Ingredient.EMPTY);

            for (int i = 0; i < inputItems.size(); i++) {
                inputItems.set(i, Ingredient.fromNetwork(buffer));
            }

            ItemStack output = buffer.readItem();
            int manaCost = buffer.readInt();  // 从缓冲区读取魔力消耗值

            return new ManaCraftingTableRecipe(recipeId, inputItems, output, manaCost);
        }

        // 将配方数据写入网络缓冲区
        @Override
        public void toNetwork(FriendlyByteBuf buffer, ManaCraftingTableRecipe recipe) {
            buffer.writeInt(recipe.inputItems.size());

            for (Ingredient ingredient : recipe.getIngredients()) {
                ingredient.toNetwork(buffer);
            }

            buffer.writeItemStack(recipe.output, false);
            buffer.writeInt(recipe.getManaCost());  // 写入魔力消耗值
        }
    }
}