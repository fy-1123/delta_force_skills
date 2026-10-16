package com.rzy.dealt_force_skills.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Scalar reads with CompoundTag's missing-key defaults, without attaching a tag. */
public final class ItemStackTagReads {
    private ItemStackTagReads() {
    }

    public static int getInt(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.getInt(key);
    }

    public static long getLong(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        return tag == null ? 0L : tag.getLong(key);
    }

    public static boolean getBoolean(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(key);
    }
}
