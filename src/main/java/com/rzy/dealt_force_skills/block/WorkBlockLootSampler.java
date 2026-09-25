package com.rzy.dealt_force_skills.block;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Keeps the existing per-roll maximum rule while allowing previews to yield between rolls. */
final class WorkBlockLootSampler {
    private final Supplier<List<ItemStack>> roll;
    private final Map<Item, ItemStack> prototypes = new LinkedHashMap<>();
    private final Map<Item, Integer> maxCounts = new HashMap<>();
    private int remaining;
    private List<ItemStack> result;

    WorkBlockLootSampler(Supplier<List<ItemStack>> roll, int rolls) {
        this.roll = roll;
        this.remaining = Math.max(1, rolls);
    }

    /** null means more rolls are pending, an empty list means finished with no drops. */
    List<ItemStack> step() {
        if (result != null) return result;
        Map<Item, Integer> counts = new HashMap<>();
        for (ItemStack stack : roll.get()) {
            if (!stack.isEmpty()) {
                prototypes.putIfAbsent(stack.getItem(), stack.copy());
                counts.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        counts.forEach((item, count) -> maxCounts.merge(item, count, Math::max));
        if (--remaining > 0) return null;
        List<ItemStack> outputs = new ArrayList<>();
        prototypes.forEach((item, prototype) -> {
            ItemStack stack = prototype.copy();
            stack.setCount(Math.max(1, maxCounts.getOrDefault(item, 1)));
            outputs.add(stack);
        });
        outputs.sort(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        result = List.copyOf(outputs);
        return result;
    }

    List<ItemStack> finish() {
        while (step() == null) { /* Actions retain their original full roll count. */ }
        return result;
    }
}
