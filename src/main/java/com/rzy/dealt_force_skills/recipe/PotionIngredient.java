package com.rzy.dealt_force_skills.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rzy.dealt_force_skills.DealtForceSkillsMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.IIngredientSerializer;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class PotionIngredient extends Ingredient {
    private final Optional<MobEffectCategory> category;
    private final boolean allContainers;
    private ItemStack[] cachedStacks;

    public static void register() {
        CraftingHelper.register(new ResourceLocation(DealtForceSkillsMod.MODID, "potion"), Serializer.INSTANCE);
    }

    public PotionIngredient(Optional<MobEffectCategory> category, boolean allContainers) {
        super(Stream.empty());
        this.category = category;
        this.allContainers = allContainers;
    }

    @Override
    public boolean test(ItemStack stack) {
        if (stack == null) {
            return false;
        }
        if (!stack.is(Items.POTION) && !(allContainers
                && (stack.is(Items.SPLASH_POTION) || stack.is(Items.LINGERING_POTION)))) {
            return false;
        }
        if (category.isEmpty()) {
            return true;
        }
        List<MobEffectInstance> effects = PotionUtils.getMobEffects(stack);
        boolean matches = false;
        for (MobEffectInstance effect : effects) {
            if (category.get() == MobEffectCategory.BENEFICIAL
                    && effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) {
                return false;
            }
            if (effect.getEffect().getCategory() == category.get()) {
                matches = true;
            }
        }
        return matches;
    }

    @Override
    public ItemStack[] getItems() {
        if (cachedStacks != null) {
            return cachedStacks;
        }
        Stream<Item> containers = allContainers
                ? Stream.of(Items.POTION, Items.SPLASH_POTION, Items.LINGERING_POTION) : Stream.of(Items.POTION);
        List<ItemStack> stacks = new ArrayList<>();
        containers.forEach(item -> {
            for (net.minecraft.world.item.alchemy.Potion potion : ForgeRegistries.POTIONS.getValues()) {
                ItemStack stack = new ItemStack(item);
                PotionUtils.setPotion(stack, potion);
                if (this.test(stack)) {
                    stacks.add(stack);
                }
            }
        });
        cachedStacks = stacks.toArray(new ItemStack[0]);
        return cachedStacks;
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public IIngredientSerializer<? extends Ingredient> getSerializer() {
        return Serializer.INSTANCE;
    }

    @Override
    public JsonElement toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", DealtForceSkillsMod.MODID + ":potion");
        category.ifPresent(cat -> json.addProperty("category", cat.name()));
        if (allContainers) {
            json.addProperty("all_containers", true);
        }
        return json;
    }

    public static final class Serializer implements IIngredientSerializer<PotionIngredient> {
        public static final Serializer INSTANCE = new Serializer();

        private Serializer() {
        }

        @Override
        public PotionIngredient parse(FriendlyByteBuf buffer) {
            boolean allContainers = buffer.readBoolean();
            boolean hasCategory = buffer.readBoolean();
            Optional<MobEffectCategory> category = hasCategory
                    ? Optional.of(buffer.readEnum(MobEffectCategory.class)) : Optional.empty();
            return new PotionIngredient(category, allContainers);
        }

        @Override
        public PotionIngredient parse(JsonObject json) {
            boolean allContainers = json.has("all_containers") && json.get("all_containers").getAsBoolean();
            Optional<MobEffectCategory> category = Optional.empty();
            if (json.has("category")) {
                try {
                    category = Optional.of(MobEffectCategory.valueOf(json.get("category").getAsString()));
                } catch (IllegalArgumentException ignored) {
                }
            }
            return new PotionIngredient(category, allContainers);
        }

        @Override
        public void write(FriendlyByteBuf buffer, PotionIngredient ingredient) {
            buffer.writeBoolean(ingredient.allContainers);
            buffer.writeBoolean(ingredient.category.isPresent());
            ingredient.category.ifPresent(buffer::writeEnum);
        }
    }
}