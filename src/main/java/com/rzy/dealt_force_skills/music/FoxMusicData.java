package com.rzy.dealt_force_skills.music;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Item-owned storage. Only server actions may consume or return physical discs. */
public final class FoxMusicData {
    public static final int CAPACITY = 2048;
    public static final int PAGE_SIZE = 6;
    private static final String ROOT = "FoxMusicPlayer";
    public enum Mode { SEQUENCE, SHUFFLE, REPEAT_ONE }
    public record Song(ResourceLocation sound, int ticks) { }

    private FoxMusicData() { }

    public static CompoundTag data(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getCompound(ROOT).copy() : new CompoundTag();
    }
    public static void save(ItemStack stack, CompoundTag data) {
        stack.getOrCreateTag().put(ROOT, data);
    }
    public static UUID id(ItemStack stack) {
        CompoundTag data = data(stack);
        if (!data.hasUUID("Id")) { data.putUUID("Id", UUID.randomUUID()); save(stack, data); }
        return data.getUUID("Id");
    }
    public static boolean matches(ItemStack stack, UUID id) {
        CompoundTag tag = data(stack);
        return tag.hasUUID("Id") && id.equals(tag.getUUID("Id"));
    }
    public static Song song(ItemStack disc, Level level) {
        if (!(disc.getItem() instanceof RecordItem record)) return null;
        SoundEvent sound = record.getSound();
        return sound == null ? null : new Song(sound.getLocation(), Math.max(1, record.getLengthInTicks()));
    }
    public static String kind(ItemStack stack, Level level) {
        Song song = song(stack, level);
        return song == null ? "" : ForgeRegistries.ITEMS.getKey(stack.getItem()) + "|" + song.sound();
    }
    public static int count(ItemStack stack) {
        return Math.min(CAPACITY, data(stack).getList("Discs", Tag.TAG_COMPOUND).size());
    }
    public static List<ItemStack> discs(ItemStack stack, Level level) {
        ListTag stored = data(stack).getList("Discs", Tag.TAG_COMPOUND);
        List<ItemStack> result = new ArrayList<>();
        Set<String> kinds = new HashSet<>();
        for (int i = 0; i < Math.min(CAPACITY, stored.size()); i++) {
            ItemStack disc = ItemStack.of(stored.getCompound(i));
            // Preserve unknown/custom discs for removal even if their playback data is temporarily unavailable.
            if (!disc.isEmpty() && kinds.add(kind(disc, level).isEmpty()
                    ? ForgeRegistries.ITEMS.getKey(disc.getItem()).toString() : kind(disc, level))) {
                disc.setCount(1); result.add(disc);
            }
        }
        return result;
    }
    public static void store(ItemStack stack, List<ItemStack> discs, Level level) {
        if (discs.size() > CAPACITY) throw new IllegalArgumentException("Disc capacity exceeded");
        CompoundTag tag = data(stack);
        ListTag list = new ListTag();
        for (ItemStack disc : discs) { ItemStack copy = disc.copy(); copy.setCount(1); list.add(copy.save(new CompoundTag())); }
        tag.put("Discs", list);
        tag.putInt("Track", Math.max(0, Math.min(Math.max(0, discs.size() - 1), tag.getInt("Track"))));
        if (discs.isEmpty()) tag.putBoolean("Playing", false);
        save(stack, tag);
    }
    public static Mode mode(CompoundTag data) {
        return Mode.values()[Math.floorMod(data.getInt("Mode"), Mode.values().length)];
    }
    public static void select(ItemStack stack, int index, boolean playing) {
        CompoundTag data = data(stack);
        data.putInt("Track", Math.max(0, Math.min(Math.max(0, count(stack) - 1), index)));
        data.putBoolean("Playing", playing && count(stack) > 0);
        data.putInt("Generation", data.getInt("Generation") + 1);
        save(stack, data);
    }
    public static ItemStack current(ItemStack stack, Level level) {
        CompoundTag tag = data(stack);
        ListTag discs = tag.getList("Discs", Tag.TAG_COMPOUND);
        int index = tag.getInt("Track");
        return index < 0 || index >= Math.min(CAPACITY, discs.size()) ? ItemStack.EMPTY : ItemStack.of(discs.getCompound(index));
    }
}
