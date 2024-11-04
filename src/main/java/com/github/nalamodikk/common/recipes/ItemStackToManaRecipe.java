package com.github.nalamodikk.common.recipes;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import com.github.nalamodikk.common.recipes.ingredients.ManaItemIngredient;

/**
 * Input: ItemStack
 * <br>
 * Output: Mana (as an integer)
 *
 * @apiNote Mana conversion recipes can be used in any slots in machines that are able to convert items into mana.
 */
public abstract class ItemStackToManaRecipe implements Predicate<@NotNull ItemStack> {

    protected final ManaItemIngredient input;
    protected final int outputMana;

    /**
     * @param id     Recipe name.
     * @param input  Input.
     * @param outputMana Output mana, must be greater than zero.
     */
    public ItemStackToManaRecipe(ResourceLocation id, ManaItemIngredient input, int outputMana) {
        this.input = Objects.requireNonNull(input, "Input cannot be null.");
        if (outputMana <= 0) {
            throw new IllegalArgumentException("Output must be greater than zero.");
        }
        this.outputMana = outputMana;
    }

    @Override
    public boolean test(ItemStack itemStack) {
        return input.test(itemStack);
    }

    /**
     * Gets the input ingredient.
     */
    public ManaItemIngredient getInput() {
        return input;
    }

    /**
     * Gets the output based on the given input.
     *
     * @param input Specific input.
     *
     * @return Output as mana.
     *
     * @apiNote While this implementation does not currently make use of the input, it is important to support it in case any future logic defines input-based outputs
     * where things like NBT may be different.
     * @implNote The passed in input should <strong>NOT</strong> be modified.
     */
    public int getOutputMana(ItemStack input) {
        return outputMana;
    }

    /**
     * For JEI, gets the output representations to display.
     *
     * @return Representation of the output, <strong>MUST NOT</strong> be modified.
     */
    public List<Integer> getOutputDefinition() {
        return Collections.singletonList(outputMana);
    }

    public boolean isIncomplete() {
        return input.hasNoMatchingInstances();
    }

    public void write(FriendlyByteBuf buffer) {
        input.write(buffer);
        buffer.writeInt(outputMana);
    }
}
