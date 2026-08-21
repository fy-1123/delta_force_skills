package com.rzy.dealt_force_skills.client.screen;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Locale;

public final class HvkResourceDisplayNames {
    private HvkResourceDisplayNames() {
    }

    public static Component structure(String rawId) {
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        return id == null ? Component.literal(rawId) : resolve(id, "structure", null);
    }

    public static Component lootTable(ResourceLocation id, String fallback) {
        return id == null ? Component.literal(fallback) : resolve(id, "loot_table", fallback);
    }

    private static Component resolve(ResourceLocation id, String kind, String fallback) {
        String dottedPath = id.getPath().replace('/', '.');
        String[] prefixes = kind.equals("structure")
                ? new String[]{"structure", "structures"}
                : new String[]{"loot_table", "loot"};

        for (String prefix : prefixes) {
            Component translated = translated(prefix + "." + id.getNamespace() + "." + dottedPath);
            if (translated != null) {
                return translated;
            }
            if (!dottedPath.equals(id.getPath())) {
                translated = translated(prefix + "." + id.getNamespace() + "." + id.getPath());
                if (translated != null) {
                    return translated;
                }
            }
        }

        Component bundled = translated("display.dealt_force_skills." + kind + "."
                + id.getNamespace() + "." + dottedPath);
        if (bundled != null) {
            return bundled;
        }
        if (fallback != null && !fallback.isBlank() && !fallback.equals(id.toString())) {
            return Component.literal(fallback);
        }
        String readable = humanize(id.getPath());
        return Component.literal(id.getNamespace().equals("minecraft")
                ? readable
                : readable + " (" + id.getNamespace() + ")");
    }

    private static Component translated(String key) {
        return I18n.exists(key) ? Component.translatable(key) : null;
    }

    private static String humanize(String path) {
        String[] parts = path.replace('/', ' ').replace('_', ' ').trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(part.substring(0, 1).toUpperCase(Locale.ROOT)).append(part.substring(1));
        }
        return result.length() == 0 ? path : result.toString();
    }
}

