package com.rzy.dealt_force_skills.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ApotheosisCompat {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String MOD_ID = "apotheosis";
    private static final String ENCH_MODULE = "dev.shadowsoffire.apotheosis.ench.EnchModule";
    private static final String AFFIX_REGISTRY = "dev.shadowsoffire.apotheosis.adventure.affix.AffixRegistry";
    private static final String AFFIX_HELPER = "dev.shadowsoffire.apotheosis.adventure.affix.AffixHelper";
    private static final String AFFIX_INSTANCE = "dev.shadowsoffire.apotheosis.adventure.affix.AffixInstance";
    private static final String AFFIX = "dev.shadowsoffire.apotheosis.adventure.affix.Affix";
    private static final String LOOT_CATEGORY = "dev.shadowsoffire.apotheosis.adventure.loot.LootCategory";
    private static final String RARITY_REGISTRY = "dev.shadowsoffire.apotheosis.adventure.loot.RarityRegistry";
    private static volatile boolean enchantmentApiUnavailable;
    private static volatile boolean affixApiUnavailable;
    private static volatile boolean affixWriteFailureLogged;

    private ApotheosisCompat() {
    }

    public record AffixEntry(String id, String name, int currentValue, int maxValue) {
    }

    public static int enchantmentMaxLevel(Enchantment enchantment, int fallback) {
        int safeFallback = Math.max(1, fallback);
        if (enchantment == null || enchantmentApiUnavailable || !ModList.get().isLoaded(MOD_ID)) {
            return safeFallback;
        }
        try {
            Class<?> module = load(ENCH_MODULE);
            Object info = invokeStatic(module, "getEnchInfo", enchantment);
            Object value = invoke(info, "getMaxLevel");
            return value instanceof Number number && number.intValue() > 0 ? number.intValue() : safeFallback;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            enchantmentApiUnavailable = true;
            LOGGER.warn("Apotheosis was detected, but its enchantment-level API was unavailable; using vanilla limits", error);
            return safeFallback;
        }
    }

    public static boolean isAffixEditingAvailable() {
        return !affixApiUnavailable && ModList.get().isLoaded(MOD_ID);
    }

    public static int affixMaxValue() {
        if (!isAffixEditingAvailable()) return 0;
        try {
            return Math.max(1, Math.round(readAffixBaseMax() * 3.0F * 100.0F));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            markAffixUnavailable(error);
            return 0;
        }
    }

    public static List<AffixEntry> listAffixes(ItemStack stack) {
        if (stack.isEmpty() || !isAffixEditingAvailable()) return List.of();
        try {
            Object registry = staticField(load(AFFIX_REGISTRY), "INSTANCE");
            Object rawKeys = invoke(registry, "getKeys");
            Object rawCurrent = invokeStatic(load(AFFIX_HELPER), "getAffixes", stack);
            if (!(rawKeys instanceof Set<?> keys) || !(rawCurrent instanceof Map<?, ?> current)) return List.of();

            int maxValue = Math.max(1, Math.round(readAffixBaseMax() * 3.0F * 100.0F));
            Object category = invokeStatic(load(LOOT_CATEGORY), "forItem", stack);
            Object rarity = validationRarity(stack);
            List<AffixEntry> entries = new ArrayList<>(keys.size());
            for (Object key : keys) {
                if (!(key instanceof ResourceLocation id)) continue;
                Object holder = invoke(registry, "holder", id);
                if (!isBound(holder)) continue;
                int currentValue = affixValue(current, holder);
                Object affix = invoke(holder, "get");
                if (currentValue == 0 && !canApplyAffix(stack, affix, category, rarity)) continue;
                String name = componentString(invoke(affix, "getName", false), id.toString());
                entries.add(new AffixEntry(id.toString(), name, currentValue, maxValue));
            }
            entries.sort(Comparator.comparing(AffixEntry::id));
            return List.copyOf(entries);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            markAffixUnavailable(error);
            return List.of();
        }
    }

    public static boolean canApplyAffix(ItemStack stack, String affixId) {
        if (stack.isEmpty() || !isAffixEditingAvailable()) return false;
        ResourceLocation id = ResourceLocation.tryParse(affixId);
        if (id == null) return false;
        try {
            Object registry = staticField(load(AFFIX_REGISTRY), "INSTANCE");
            Object holder = invoke(registry, "holder", id);
            if (!isBound(holder)) return false;
            return canApplyAffix(stack, invoke(holder, "get"),
                    invokeStatic(load(LOOT_CATEGORY), "forItem", stack), validationRarity(stack));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            markAffixUnavailable(error);
            return false;
        }
    }

    public static boolean applyAffix(ItemStack stack, String affixId, int requestedValue) {
        if (stack.isEmpty() || !isAffixEditingAvailable()) return false;
        ResourceLocation id = ResourceLocation.tryParse(affixId);
        if (id == null) return false;
        ItemStack backup = null;
        try {
            int maxValue = Math.max(1, Math.round(readAffixBaseMax() * 3.0F * 100.0F));
            int value = Math.max(0, Math.min(maxValue, requestedValue));
            Class<?> registryClass = load(AFFIX_REGISTRY);
            Object registry = staticField(registryClass, "INSTANCE");
            Object holder = invoke(registry, "holder", id);
            if (!isBound(holder)) return false;

            Class<?> helper = load(AFFIX_HELPER);
            Object rawCurrent = invokeStatic(helper, "getAffixes", stack);
            if (!(rawCurrent instanceof Map<?, ?> current)) return false;
            Map<Object, Object> updated = new LinkedHashMap<>();
            current.forEach(updated::put);
            Object existingKey = findAffixKey(updated, holder);
            Object existing = existingKey == null ? null : updated.get(existingKey);
            int currentValue = affixValue(updated, holder);
            if (currentValue == value) return false;

            if (value == 0) {
                backup = stack.copy();
                updated.remove(existingKey);
                invokeStatic(helper, "setAffixes", stack, updated);
            } else {
                Object rarity = validationRarity(stack);
                if (!canApplyAffix(stack, invoke(holder, "get"),
                        invokeStatic(load(LOOT_CATEGORY), "forItem", stack), rarity)) return false;
                backup = stack.copy();
                rarity = ensureRarity(stack);
                Object replacement = newAffixInstance(holder, stack, rarity, value / 100.0F);
                if (existing == null) {
                    invokeStatic(helper, "applyAffix", stack, replacement);
                } else {
                    updated.remove(existingKey);
                    invokeStatic(helper, "setAffixes", stack, updated);
                    invokeStatic(helper, "applyAffix", stack, replacement);
                }
            }
            Object rawWritten = invokeStatic(helper, "getAffixes", stack);
            int writtenValue = rawWritten instanceof Map<?, ?> written ? affixValue(written, holder) : -1;
            if (writtenValue != value) {
                throw new IllegalStateException("Apotheosis affix write did not persist: " + id
                        + " requested=" + value + " readback=" + writtenValue);
            }
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
            if (backup != null) restoreStackTag(stack, backup);
            logAffixWriteFailure(error);
            return false;
        }
    }

    private static void restoreStackTag(ItemStack stack, ItemStack backup) {
        stack.setTag(backup.getTag() == null ? null : backup.getTag().copy());
    }

    private static boolean canApplyAffix(ItemStack stack, Object affix, Object category, Object rarity)
            throws ReflectiveOperationException {
        return Boolean.TRUE.equals(invoke(affix, "canApplyTo", stack, category, invoke(rarity, "get")));
    }

    private static Object validationRarity(ItemStack stack) throws ReflectiveOperationException {
        Object rarity = invokeStatic(load(AFFIX_HELPER), "getRarity", stack);
        return isBound(rarity) ? rarity : invokeStatic(load(RARITY_REGISTRY), "getMinRarity");
    }

    private static Object ensureRarity(ItemStack stack) throws ReflectiveOperationException {
        Object rarity = invokeStatic(load(AFFIX_HELPER), "getRarity", stack);
        if (isBound(rarity)) return rarity;
        rarity = invokeStatic(load(RARITY_REGISTRY), "getMinRarity");
        invokeStatic(load(AFFIX_HELPER), "setRarity", stack, invoke(rarity, "get"));
        return rarity;
    }

    private static Object newAffixInstance(Object holder, ItemStack stack, Object rarity, float level)
            throws ReflectiveOperationException {
        Class<?> type = load(AFFIX_INSTANCE);
        Object[] arguments = {holder, stack, rarity, level};
        for (Constructor<?> constructor : type.getConstructors()) {
            if (compatible(constructor.getParameterTypes(), arguments)) return constructor.newInstance(arguments);
        }
        throw new NoSuchMethodException(AFFIX_INSTANCE + " compatible constructor");
    }

    private static int affixValue(Map<?, ?> affixes, Object holder) throws ReflectiveOperationException {
        Object instance = findAffixInstance(affixes, holder);
        return instance == null ? 0
                : Math.max(0, Math.round(((Number) invoke(instance, "level")).floatValue() * 100.0F));
    }

    private static Object findAffixInstance(Map<?, ?> affixes, Object holder) throws ReflectiveOperationException {
        Object key = findAffixKey(affixes, holder);
        return key == null ? null : affixes.get(key);
    }

    private static Object findAffixKey(Map<?, ?> affixes, Object holder) throws ReflectiveOperationException {
        if (affixes.get(holder) != null) return holder;
        Object id = invoke(holder, "getId");
        for (Map.Entry<?, ?> entry : affixes.entrySet()) {
            if (entry.getKey() != null && id.equals(invoke(entry.getKey(), "getId"))) return entry.getKey();
        }
        return null;
    }

    private static float readAffixBaseMax() throws ReflectiveOperationException {
        try {
            Object value = staticField(load(AFFIX), "MAX_LEVEL");
            if (value instanceof Number number && number.floatValue() > 0.0F) return number.floatValue();
        } catch (NoSuchFieldException ignored) {
            // Apotheosis 6/7 use normalized affix levels in the range [0, 1].
        }
        return 1.0F;
    }

    private static void markAffixUnavailable(Throwable error) {
        if (!affixApiUnavailable) {
            affixApiUnavailable = true;
            LOGGER.warn("Apotheosis was detected, but its affix API was unavailable; affix editing is disabled", error);
        }
    }

    private static void logAffixWriteFailure(Throwable error) {
        if (!affixWriteFailureLogged) {
            affixWriteFailureLogged = true;
            LOGGER.warn("Apotheosis rejected an affix change; this change was ignored", error);
        }
    }

    private static boolean isBound(Object holder) throws ReflectiveOperationException {
        return holder != null && Boolean.TRUE.equals(invoke(holder, "isBound"));
    }

    private static String componentString(Object value, String fallback) {
        return value instanceof Component component && !component.getString().isBlank() ? component.getString() : fallback;
    }

    private static Class<?> load(String name) throws ClassNotFoundException {
        return Class.forName(name, true, ApotheosisCompat.class.getClassLoader());
    }

    private static Object staticField(Class<?> type, String name) throws ReflectiveOperationException {
        Field field = type.getField(name);
        return field.get(null);
    }

    private static Object invokeStatic(Class<?> type, String name, Object... arguments) throws ReflectiveOperationException {
        return findMethod(type, name, arguments).invoke(null, arguments);
    }

    private static Object invoke(Object target, String name, Object... arguments) throws ReflectiveOperationException {
        if (target == null) throw new NullPointerException(name + " target");
        return findMethod(target.getClass(), name, arguments).invoke(target, arguments);
    }

    private static Method findMethod(Class<?> type, String name, Object[] arguments) throws NoSuchMethodException {
        for (Method method : type.getMethods()) {
            if (method.getName().equals(name) && compatible(method.getParameterTypes(), arguments)) return method;
        }
        throw new NoSuchMethodException(type.getName() + "." + name);
    }

    private static boolean compatible(Class<?>[] parameters, Object[] arguments) {
        if (parameters.length != arguments.length) return false;
        for (int i = 0; i < parameters.length; i++) {
            if (arguments[i] == null) {
                if (parameters[i].isPrimitive()) return false;
            } else if (!boxed(parameters[i]).isInstance(arguments[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == boolean.class) return Boolean.class;
        if (type == byte.class) return Byte.class;
        if (type == short.class) return Short.class;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == float.class) return Float.class;
        if (type == double.class) return Double.class;
        if (type == char.class) return Character.class;
        return type;
    }
}
