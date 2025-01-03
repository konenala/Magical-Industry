package com.github.nalamodikk.common.recipe.fuel;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public class FuelRecipe implements Recipe<Container> {
    private final ResourceLocation id;
    private final String itemId;
    private final int manaRate;
    private final int energyRate;
    private final int burnTime;  // 新增屬性


    public FuelRecipe(ResourceLocation id, String itemId, int manaRate, int energyRate ,int burnTime) {
        this.id = id;
        this.itemId = itemId;
        this.manaRate = manaRate;
        this.energyRate = energyRate;
        this.burnTime = burnTime;

    }
    public int getBurnTime() {
        return burnTime;
    }
    @Override
    public boolean matches(Container container, Level level) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack itemStack = container.getItem(i);
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
            if (itemId != null && itemId.toString().equals(this.itemId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ItemStack assemble(Container container, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FuelRecipeSerializer.INSTANCE;
    }

    @Override
    public RecipeType<?> getType() {
        return FuelRecipeType.INSTANCE;
    }

    public int getManaRate() {
        return manaRate;
    }

    public int getEnergyRate() {
        return energyRate;
    }

    public static class FuelRecipeSerializer implements RecipeSerializer<FuelRecipe> {
        public static final FuelRecipeSerializer INSTANCE = new FuelRecipeSerializer();

        @Override
        public FuelRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            String itemId = json.getAsJsonObject("ingredient").get("item").getAsString();
            int manaRate = json.get("mana").getAsInt();
            int energyRate = json.get("energy").getAsInt();
            int burnTime = json.has("burn_time") ? json.get("burn_time").getAsInt() : 200; // 默認燃燒時間
            return new FuelRecipe(recipeId, itemId, manaRate, energyRate, burnTime);
        }

        @Override
        public FuelRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            String itemId = buffer.readUtf();
            int manaRate = buffer.readInt();
            int energyRate = buffer.readInt();
            int burnTime = buffer.readInt();  // 新增的從 buffer 中讀取 burnTime
            return new FuelRecipe(recipeId, itemId, manaRate, energyRate, burnTime);
        }


        @Override
        public void toNetwork(FriendlyByteBuf buffer, FuelRecipe recipe) {
            buffer.writeUtf(recipe.itemId);
            buffer.writeInt(recipe.manaRate);
            buffer.writeInt(recipe.energyRate);
            buffer.writeInt(recipe.burnTime);  // 新增的將 burnTime 寫入 buffer
        }

    }

    public static class FuelRecipeType implements RecipeType<FuelRecipe> {
        public static final FuelRecipeType INSTANCE = new FuelRecipeType();
        private FuelRecipeType() {
        }
    }
}
