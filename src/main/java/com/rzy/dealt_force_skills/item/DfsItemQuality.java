package com.rzy.dealt_force_skills.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public enum DfsItemQuality {
    WHITE("white", 0, 0xFFFFFF),
    GREEN("green", 1, 0x55FF55),
    BLUE("blue", 2, 0x55AAFF),
    PURPLE("purple", 3, 0xAA55FF),
    GOLD("gold", 4, 0xFFD34D),
    RED("red", 5, 0xFF5555),
    COLORFUL("colorful", 6, 0xFF55FF);

    private final String key;
    private final int tier;
    private final int color;

    DfsItemQuality(String key, int tier, int color) {
        this.key = key;
        this.tier = tier;
        this.color = color;
    }

    public String key() {
        return key;
    }

    public int tier() {
        return tier;
    }

    public int color() {
        return color;
    }

    public static DfsItemQuality of(ItemStack stack) {
        if (stack.isEmpty()) {
            return WHITE;
        }
        if (stack.getItem() instanceof QualityTooltipItem qualityItem) {
            return qualityItem.quality();
        }
        if (stack.getItem() instanceof DfsEquipmentItem equipmentItem) {
            return equipmentItem.profile().quality();
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && "tamarind".equals(id.getNamespace())) {
            String name = stack.getHoverName().getString();
            int marker = name.indexOf('\u00a7');
            if (marker >= 0 && marker + 1 < name.length()) {
                return tamarindQuality(Character.toLowerCase(name.charAt(marker + 1)));
            }
        }
        return WHITE;
    }

    static DfsItemQuality tamarindQuality(char colorCode) {
        return switch (colorCode) {
            case 'a' -> GREEN;
            case '9' -> BLUE;
            case '5' -> PURPLE;
            case '6' -> GOLD;
            case 'c' -> RED;
            case 'f' -> WHITE;
            default -> WHITE;
        };
    }
}
