package com.rzy.dealt_force_skills.registry;

import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.brewing.IBrewingRecipe;

import java.util.function.Supplier;
import java.util.List;

public final class ModBrewingRecipes {
    private static final List<IBrewingRecipe> RECIPES = List.of(
            new PotionCategoryRecipe(MobEffectCategory.HARMFUL, () -> ModItems.RAVEN_SHADOW_EXTRACT.get()),
            new PotionCategoryRecipe(MobEffectCategory.BENEFICIAL, () -> ModItems.EXPERIMENTAL_IRON_CURTAIN_CATALYST.get()),
            new CompoundRecipe());

    private ModBrewingRecipes() {
    }

    public static void register() {
        RECIPES.forEach(BrewingRecipeRegistry::addRecipe);
    }

    public static ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        for (IBrewingRecipe recipe : RECIPES) {
            ItemStack output = recipe.getOutput(input, ingredient);
            if (!output.isEmpty()) {
                return output;
            }
        }
        return ItemStack.EMPTY;
    }

    private static boolean isPotionInput(ItemStack stack) {
        return stack.is(Items.POTION) || stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION);
    }

    private static class PotionCategoryRecipe implements IBrewingRecipe {
        private final MobEffectCategory category;
        private final Supplier<Item> output;

        private PotionCategoryRecipe(MobEffectCategory category, Supplier<Item> output) {
            this.category = category;
            this.output = output;
        }

        @Override
        public boolean isInput(ItemStack input) {
            return isPotionInput(input) && PotionUtils.getMobEffects(input).stream()
                    .anyMatch(effect -> effect.getEffect().getCategory() == category);
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.SUSPICIOUS_STEW);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            return isInput(input) && isIngredient(ingredient) ? new ItemStack(output.get()) : ItemStack.EMPTY;
        }
    }

    private static class CompoundRecipe implements IBrewingRecipe {
        @Override
        public boolean isInput(ItemStack input) {
            return input.is(ModItems.RAVEN_SHADOW_EXTRACT.get())
                    || input.is(ModItems.EXPERIMENTAL_IRON_CURTAIN_CATALYST.get());
        }

        @Override
        public boolean isIngredient(ItemStack ingredient) {
            return ingredient.is(Items.SUSPICIOUS_STEW);
        }

        @Override
        public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
            return isInput(input) && isIngredient(ingredient)
                    ? new ItemStack(ModItems.PROTOTYPE_MADNESS_COMPOUND.get())
                    : ItemStack.EMPTY;
        }
    }
}
