package com.rzy.dealt_force_skills.registry;

import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import com.rzy.dealt_force_skills.recipe.HvkEnchantInfusionRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipeSerializers {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DealtForceSkillsMod.MODID);
    public static final RegistryObject<RecipeSerializer<?>> HVK_ENCHANT_INFUSION =
            RECIPE_SERIALIZERS.register("hvk_enchant_infusion",
                    () -> new SimpleCraftingRecipeSerializer<>(HvkEnchantInfusionRecipe::new));
    private ModRecipeSerializers() {}
}