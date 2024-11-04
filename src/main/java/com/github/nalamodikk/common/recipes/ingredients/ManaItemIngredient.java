package com.github.nalamodikk.common.recipes.ingredients;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Base implementation for how Mana-related Item Ingredients are handled.
 * <p>
 * Create instances of this using a custom Ingredient Creator for mana conversion recipes.
 */
public abstract class ManaItemIngredient implements ManaInputIngredient<@NotNull ItemStack> {
}
