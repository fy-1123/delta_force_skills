package com.rzy.dealt_force_skills.boss;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

public final class AcceleratedTroopsEquipment {
    private static final ResourceLocation TACZ_GUN_ITEM =
            ResourceLocation.fromNamespaceAndPath("tacz", "modern_kinetic_gun");
    private static final ResourceLocation TACZ_AMMO_BOX_ITEM =
            ResourceLocation.fromNamespaceAndPath("tacz", "ammo_box");
    private static final String M4A1_SNBT =
            "{Count:1b,id:\"tacz:modern_kinetic_gun\",tag:{AttachmentEXTENDED_MAG:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:ammo_mod_i\"}},AttachmentGRIP:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:grip_vertical_ranger\"}},AttachmentLASER:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:laser_lopro\"}},AttachmentMUZZLE:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:muzzle_brake_cyclone_d2\"}},AttachmentSCOPE:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:sight_coyote\"}},AttachmentSTOCK:{Count:1b,id:\"tacz:attachment\",tag:{AttachmentId:\"tacz:stock_carbon_bone_c5\"}},GunCurrentAmmoCount:60,GunFireMode:\"AUTO\",GunId:\"tacz:m4a1\",HasBulletInBarrel:1b}}";
    private static final String AMMO_BOX_SNBT =
            "{Count:1b,id:\"tacz:ammo_box\",tag:{AmmoCount:114,AmmoId:\"tacz:556x45\",Level:2}}";

    private AcceleratedTroopsEquipment() {
    }

    public static ItemStack createM4A1() {
        return createM4A1(true);
    }

    public static ItemStack createDroppedM4A1() {
        return createM4A1(false);
    }

    private static ItemStack createM4A1(boolean unlimitedReserve) {
        ItemStack parsed = parse(M4A1_SNBT);
        if (!parsed.isEmpty()) {
            prepareM4A1(parsed, unlimitedReserve);
            return parsed;
        }
        Item item = ForgeRegistries.ITEMS.getValue(TACZ_GUN_ITEM);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("GunId", "tacz:m4a1");
        tag.putString("GunFireMode", "AUTO");
        putAttachment(tag, "AttachmentEXTENDED_MAG", "tacz:ammo_mod_i");
        putAttachment(tag, "AttachmentGRIP", "tacz:grip_vertical_ranger");
        putAttachment(tag, "AttachmentLASER", "tacz:laser_lopro");
        putAttachment(tag, "AttachmentMUZZLE", "tacz:muzzle_brake_cyclone_d2");
        putAttachment(tag, "AttachmentSCOPE", "tacz:sight_coyote");
        putAttachment(tag, "AttachmentSTOCK", "tacz:stock_carbon_bone_c5");
        prepareM4A1(stack, unlimitedReserve);
        return stack;
    }

    public static ItemStack createAmmoBox() {
        ItemStack parsed = parse(AMMO_BOX_SNBT);
        if (!parsed.isEmpty()) {
            return parsed;
        }
        Item item = ForgeRegistries.ITEMS.getValue(TACZ_AMMO_BOX_ITEM);
        if (item == null || item == Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(item);
        CompoundTag tag = stack.getOrCreateTag();
        tag.putInt("AmmoCount", 114);
        tag.putString("AmmoId", "tacz:556x45");
        tag.putInt("Level", 2);
        return stack;
    }

    public static void primeM4A1(ItemStack stack) {
        prepareM4A1(stack, true);
    }

    private static void prepareM4A1(ItemStack stack, boolean unlimitedReserve) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString("GunId", "tacz:m4a1");
        tag.putString("GunFireMode", "AUTO");
        tag.putInt("GunCurrentAmmoCount", Math.max(60, tag.getInt("GunCurrentAmmoCount")));
        tag.putBoolean("HasBulletInBarrel", true);
        if (unlimitedReserve) {
            tag.putInt("DummyAmmo", Math.max(9999, tag.getInt("DummyAmmo")));
            tag.putInt("MaxDummyAmmo", Math.max(9999, tag.getInt("MaxDummyAmmo")));
        } else {
            tag.putInt("DummyAmmo", 114);
            tag.putInt("MaxDummyAmmo", 114);
        }
    }

    private static ItemStack parse(String snbt) {
        try {
            return ItemStack.of(TagParser.parseTag(snbt));
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static void putAttachment(CompoundTag gunTag, String slotKey, String attachmentId) {
        CompoundTag attachment = new CompoundTag();
        attachment.putString("id", "tacz:attachment");
        attachment.putByte("Count", (byte) 1);
        CompoundTag attachmentTag = new CompoundTag();
        attachmentTag.putString("AttachmentId", attachmentId);
        attachment.put("tag", attachmentTag);
        gunTag.put(slotKey, attachment);
    }
}
