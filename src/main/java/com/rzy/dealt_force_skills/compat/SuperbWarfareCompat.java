package com.rzy.dealt_force_skills.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.nbt.CompoundTag;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Array;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SuperbWarfareCompat {
    private static final String MOD_ID = "superbwarfare";
    private static final String VEHICLE_ENTITY_CLASS = "com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity";
    private static final float MAGNETIC_BOMB_MAX_HEALTH_FRACTION = 0.25F;

    private static volatile boolean vehicleClassResolved;
    private static volatile Class<?> vehicleClass;
    private static volatile Method getMaxHealthMethod;
    private static final int COMMANDER_VEHICLE_VALUE = 1_000_000;
    private static final double COMMANDER_WEAPON_SPEED = 10.0D;
    private static final String AMMO_TUNED = "CommanderAmmoTuned";
    private static final String AMMO_PROFILE = "CommanderWeaponAmmoProfile";
    private static final String AMMO_DATA_PROFILE = "CommanderVehicleAmmoDataProfile";
    private static final String FORCED_CANNOT_FIRE = "CommanderCompatForcedCannotFire";
    private static final String FORCED_GUN_STOP = "CommanderCompatForcedGunStop";
    private static final Set<String> VEHICLE_TIMER_ACCESSORS = Set.of(
            "FIRE_TIME", "CANNON_FIRE_TIME", "GUN_FIRE_TIME", "COOL_DOWN", "CHARGING_TIME"
    );
    private static final Set<String> VEHICLE_AMMO_ACCESSORS = Set.of(
            "AMMO", "AMMO_COUNT", "MG_AMMO", "MAX_AMMO", "MISSILE_COUNT",
            "LOADED_ROCKET", "LOADED_BOMB", "LOADED_MISSILE", "LOADED_DRONE"
    );
    private static final Set<String> VEHICLE_HEAT_ACCESSORS = Set.of("HEAT", "COAX_HEAT");
    private static final Map<Class<?>, List<Field>> VEHICLE_INSTANCE_TIMERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> VEHICLE_DATA_TIMERS = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> VEHICLE_AMMO_DATA = new ConcurrentHashMap<>();
    private static final Map<Class<?>, List<Field>> VEHICLE_HEAT_DATA = new ConcurrentHashMap<>();
    private SuperbWarfareCompat() {
    }

    public static void damageVehicles(ServerLevel level, Vec3 center, double radius, DamageSource source,
                                      Entity excludedEntity, float magneticBombEquivalents,
                                      boolean requireLineOfSight) {
        if (level == null || center == null || source == null || radius <= 0.0D
                || magneticBombEquivalents <= 0.0F || !isLoaded()) {
            return;
        }

        AABB searchBox = new AABB(center, center).inflate(radius);
        for (Entity entity : level.getEntities((Entity) null, searchBox,
                entity -> entity != excludedEntity && isVehicle(entity) && !entity.isRemoved())) {
            AABB bounds = entity.getBoundingBox();
            Vec3 hitPoint = closestPoint(center, bounds);
            double distance = hitPoint.distanceTo(center);
            if (distance > radius) {
                continue;
            }
            if (requireLineOfSight && !hasLineOfSight(level, center, hitPoint, entity)) {
                continue;
            }

            float maxHealth = maxHealth(entity);
            if (maxHealth <= 0.0F) {
                continue;
            }
            float falloff = Math.max(0.0F, 1.0F - (float) (distance / radius));
            float damage = maxHealth * MAGNETIC_BOMB_MAX_HEALTH_FRACTION * magneticBombEquivalents * falloff;
            if (damage <= 0.0F) {
                continue;
            }
            entity.hurt(source, damage);
            entity.hurtMarked = true;
        }
    }

    public static void removeVehicles(List<Entity> entities) {
        if (entities == null || entities.isEmpty() || !isLoaded()) {
            return;
        }
        entities.removeIf(SuperbWarfareCompat::isVehicle);
    }

    public static boolean isVehicle(Entity entity) {
        Class<?> type = vehicleClass();
        return entity != null && type != null && type.isInstance(entity);
    }

    public static List<net.minecraft.resources.ResourceLocation> registeredVehicleIds(Level level) {
        if (!isLoaded() || level == null) {
            return List.of();
        }
        return ForgeRegistries.ENTITY_TYPES.getValues().stream()
                .map(ForgeRegistries.ENTITY_TYPES::getKey)
                .filter(id -> id != null && MOD_ID.equals(id.getNamespace()))
                .filter(id -> {
                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                    Entity candidate = type == null ? null : type.create(level);
                    boolean vehicle = candidate != null && isVehicle(candidate);
                    if (candidate != null) {
                        candidate.discard();
                    }
                    return vehicle;
                })
                .sorted(Comparator.comparing(net.minecraft.resources.ResourceLocation::toString))
                .toList();
    }

    public static EntityType<?> registeredVehicleType(ServerLevel level) {
        List<net.minecraft.resources.ResourceLocation> ids = registeredVehicleIds(level);
        return ids.isEmpty() ? null : registeredVehicleType(level, ids.get(0).toString());
    }

    public static EntityType<?> registeredVehicleType(ServerLevel level, String requestedId) {
        if (level == null || !isLoaded()) {
            return null;
        }
        if (requestedId == null || requestedId.isBlank()) {
            return null;
        }
        net.minecraft.resources.ResourceLocation id;
        try {
            id = net.minecraft.resources.ResourceLocation.parse(requestedId);
        } catch (RuntimeException ignored) {
            return null;
        }
        if (!MOD_ID.equals(id.getNamespace())) {
            return null;
        }
        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
        if (type == null) {
            return null;
        }
        Entity candidate = type.create(level);
        boolean vehicle = candidate != null && isVehicle(candidate);
        if (candidate != null) {
            candidate.discard();
        }
        return vehicle ? type : null;
    }

    /** Marks a spawned Commander vehicle for the generic protection/maintenance hooks. */
    public static void configureCommanderVehicle(Entity entity, String markerTag) {
        if (!isVehicle(entity) || entity == null) {
            return;
        }
        entity.getPersistentData().putBoolean(markerTag, true);
        if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
            living.setHealth(living.getMaxHealth());
        }
        setEnergy(entity, COMMANDER_VEHICLE_VALUE);
        tuneWeapons(entity);
        accelerateVehicleTimers(entity, COMMANDER_WEAPON_SPEED, COMMANDER_WEAPON_SPEED);
    }

    public static boolean isCommanderVehicle(Entity entity, String markerTag) {
        return entity != null && isVehicle(entity) && entity.getPersistentData().getBoolean(markerTag);
    }

    /** Applies the same live fire/reload multipliers used by the TaCZ bridge to mounted vehicles. */
    public static void applyWeaponSpeedModifiers(Entity entity, double fireRate, double reloadRate) {
        if (!isVehicle(entity)) {
            return;
        }
        accelerateVehicleTimers(entity, fireRate, reloadRate);
        maintainGunDataVehicleWeapons(entity, fireRate, reloadRate, false);
    }

    /** Applies TaCZ-equivalent gun property, timer, and disable state to SuperbWarfare handguns. */
    public static void applyPlayerWeaponModifiers(Player player, double fireRate, double reloadRate,
                                                  double aimRate, double aimPenalty, double boltRate,
                                                  boolean disabled) {
        if (player == null || !isLoaded()) {
            return;
        }
        double effectiveAimRate = positiveMultiplier(aimRate) / positiveMultiplier(aimPenalty);
        applyGunDataModifiers(player, player.getMainHandItem(), fireRate, reloadRate,
                effectiveAimRate, boltRate, disabled);
        if (player.getOffhandItem() != player.getMainHandItem()) {
            applyGunDataModifiers(player, player.getOffhandItem(), fireRate, reloadRate,
                    effectiveAimRate, boltRate, disabled);
        }
    }

    /** Advances SuperbWarfare's client-only ADS and mounted-fire gates. */
    public static void applyClientWeaponModifiers(Player player, double fireRate, double aimRate,
                                                  double aimPenalty, boolean commanderVehicle) {
        if (player == null || !isLoaded()) {
            return;
        }
        adjustClientAim(player.getMainHandItem(), aimRate, aimPenalty);
        Entity vehicle = player.getVehicle();
        if (isVehicle(vehicle)) {
            double effectiveFireRate = positiveMultiplier(fireRate)
                    + (commanderVehicle ? COMMANDER_WEAPON_SPEED - 1.0D : 0.0D);
            adjustClientVehicleTimer(effectiveFireRate);
        }
    }

    public static void setVehicleWeaponDisabled(Entity entity, boolean disabled) {
        if (!isVehicle(entity)) {
            return;
        }
        CompoundTag data = entity.getPersistentData();
        if (disabled) {
            boolean changed = writeBooleanField(entity, "cannotFire", true);
            changed |= writeBooleanField(entity, "cannotFireCoax", true);
            if (changed) {
                data.putBoolean(FORCED_CANNOT_FIRE, true);
            }
        } else if (data.getBoolean(FORCED_CANNOT_FIRE)) {
            writeBooleanField(entity, "cannotFire", false);
            writeBooleanField(entity, "cannotFireCoax", false);
            data.remove(FORCED_CANNOT_FIRE);
        }
    }

    public static void maintainCommanderVehicle(Entity entity, String markerTag) {
        if (!isCommanderVehicle(entity, markerTag)) {
            return;
        }
        if (entity.tickCount % 20 == 0) {
            if (entity instanceof net.minecraft.world.entity.LivingEntity living) {
                living.heal(Math.max(0.0F, living.getMaxHealth() * 0.04F));
            } else {
                invokeNumeric(entity, "heal", maxHealth(entity) * 0.04F);
            }
        }
        setEnergy(entity, COMMANDER_VEHICLE_VALUE);
        tuneWeapons(entity);
        accelerateVehicleTimers(entity, COMMANDER_WEAPON_SPEED, COMMANDER_WEAPON_SPEED);
        maintainGunDataVehicleWeapons(entity,
                COMMANDER_WEAPON_SPEED, COMMANDER_WEAPON_SPEED, true);
    }

    /**
     * Uses SuperbWarfare's own container block so its normal opening animation and
     * delayed vehicle deployment remain intact. Reflection keeps this optional.
     */
    public static boolean spawnVehicleContainer(ServerLevel level, BlockPos pos, Direction facing,
                                                 EntityType<?> type, String markerTag) {
        if (level == null || pos == null || type == null || !isLoaded()) {
            return false;
        }
        Block block = ForgeRegistries.BLOCKS.getValue(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "container"));
        if (block == null || !level.setBlock(pos, block.defaultBlockState(), 3)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity == null || !writeContainerPayload(blockEntity, level, type, markerTag)) {
            level.removeBlock(pos, false);
            return false;
        }

        BlockState state = level.getBlockState(pos);
        for (Property<?> property : state.getProperties()) {
            if ("facing".equals(property.getName()) && facing != null) {
                state = setProperty(state, property, facing);
            } else if ("opened".equals(property.getName())) {
                state = setProperty(state, property, Boolean.TRUE);
            }
        }
        level.setBlock(pos, state, 3);
        blockEntity.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
        return true;
    }

    private static boolean writeContainerPayload(BlockEntity blockEntity, ServerLevel level,
                                                  EntityType<?> type, String markerTag) {
        Entity vehicle = type.create(level);
        if (vehicle == null || !isVehicle(vehicle)) {
            return false;
        }
        configureCommanderVehicle(vehicle, markerTag);
        CompoundTag entityTag = new CompoundTag();
        vehicle.saveWithoutId(entityTag);
        vehicle.discard();
        try {
            Field entityType = blockEntity.getClass().getField("entityType");
            Field payload = blockEntity.getClass().getField("entityTag");
            entityType.set(blockEntity, type);
            payload.set(blockEntity, entityTag);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static BlockState setProperty(BlockState state, Property<?> property, Object value) {
        try {
            if (property instanceof DirectionProperty directionProperty && value instanceof Direction direction) {
                return state.setValue(directionProperty, direction);
            }
            if (property instanceof BooleanProperty booleanProperty && value instanceof Boolean flag) {
                return state.setValue(booleanProperty, flag);
            }
            return state;
        } catch (IllegalArgumentException ignored) {
            return state;
        }
    }

    private static void invokeNumeric(Entity entity, String name, int value) {
        invokeNumeric(entity, name, (double) value);
    }

    private static void invokeNumeric(Entity entity, String name, double value) {
        for (Method method : entity.getClass().getMethods()) {
            if (!method.getName().equals(name) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameter = method.getParameterTypes()[0];
            Object converted;
            if (parameter == int.class || parameter == Integer.class) converted = (int) Math.round(value);
            else if (parameter == long.class || parameter == Long.class) converted = (long) value;
            else if (parameter == float.class || parameter == Float.class) converted = (float) value;
            else if (parameter == double.class || parameter == Double.class) converted = value;
            else continue;
            try {
                method.invoke(entity, converted);
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException ignored) {
                // Optional SuperbWarfare versions expose different public setter signatures.
            }
        }
    }

    private static void setEnergy(Entity entity, int requested) {
        try {
            Method storage = entity.getClass().getMethod("getEnergyStorage");
            Object energy = storage.invoke(entity);
            if (energy != null) {
                Method receive = energy.getClass().getMethod("receiveEnergy", int.class, boolean.class);
                int max = Math.max(0, ((Number) entity.getClass().getMethod("getMaxEnergy").invoke(entity)).intValue());
                receive.invoke(energy, Math.min(requested, max), false);
                return;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
        try {
            Class<?> current = entity.getClass();
            while (current != null) {
                try {
                    Method setter = current.getDeclaredMethod("setEnergy", int.class);
                    setter.setAccessible(true);
                    int max = ((Number) entity.getClass().getMethod("getMaxEnergy").invoke(entity)).intValue();
                    setter.invoke(entity, Math.min(requested, Math.max(0, max)));
                    return;
                } catch (NoSuchMethodException ignored) {
                    current = current.getSuperclass();
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void tuneWeapons(Entity entity) {
        CompoundTag persistent = entity.getPersistentData();
        Object weapons = invokeNoArg(entity, "getAllWeapons");
        if (weapons == null) {
            weapons = readField(entity, "availableWeapons");
        }
        if (weapons != null && weapons.getClass().isArray()) {
            CompoundTag profile = persistent.getCompound(AMMO_PROFILE);
            tuneWeaponArray(weapons, profile, new int[] {0}, new IdentityHashMap<>());
            persistent.put(AMMO_PROFILE, profile);
        }
        refillVehicleAmmoData(entity, persistent);
        persistent.putBoolean(AMMO_TUNED, true);
        clearHeat(entity);
    }

    private static void maintainGunDataVehicleWeapons(Entity entity, double fireRate,
                                                      double reloadRate, boolean commanderSupply) {
        Object rawMap = invokeNoArg(entity, "getGunDataMap");
        if (!(rawMap instanceof Map<?, ?> source) || source.isEmpty()) {
            return;
        }
        CompoundTag profile = entity.getPersistentData().getCompound(AMMO_PROFILE);
        Map<Object, Object> updated = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            Object gunData = entry.getValue();
            if (gunData == null) {
                continue;
            }
            GunModifierState state = new GunModifierState();
            state.fireRate = positiveMultiplier(fireRate);
            state.reloadRate = positiveMultiplier(reloadRate);
            state.boltRate = Math.max(state.fireRate, state.reloadRate);
            applyGunPropertyModifiers(gunData, state);
            reduceGunTimers(gunData, state);
            if (commanderSupply) {
                String key = "gun_data_" + String.valueOf(entry.getKey());
                int currentMagazine = readGunIntProperty(gunData, "MAGAZINE");
                int base = profile.contains(key, net.minecraft.nbt.Tag.TAG_INT)
                        ? Math.max(1, profile.getInt(key)) : Math.max(1, currentMagazine);
                if (!profile.contains(key, net.minecraft.nbt.Tag.TAG_INT) || profile.getInt(key) <= 0) {
                    profile.putInt(key, base);
                }
                int desired = scaleAmmo(base);
                setGunNumberValue(gunData, "ammo", desired);
                setGunNumberValue(gunData, "virtualAmmo", desired);
                setGunNumberValue(gunData, "backupAmmoCount", desired);
                setGunNumberValue(gunData, "heat", 0.0D);
                setGunBooleanValue(gunData, "overHeat", false);
            }
            invokeNoArg(gunData, "save");
            updated.put(entry.getKey(), gunData);
        }
        if (commanderSupply) {
            entity.getPersistentData().put(AMMO_PROFILE, profile);
        }
        invokeMapSetter(entity, "setGunDataMap", updated);
    }

    private static int readGunIntProperty(Object gunData, String propertyName) {
        try {
            Class<?> propClass = Class.forName("com.atsuishio.superbwarfare.data.gun.GunProp");
            Object property = propClass.getField(propertyName).get(null);
            Object value = gunData.getClass().getMethod("get", propClass).invoke(gunData, property);
            return value instanceof Number number ? number.intValue() : 0;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return 0;
        }
    }

    private static void tuneWeaponArray(Object array, CompoundTag profile,
                                        int[] index, IdentityHashMap<Object, Boolean> visited) {
        int length = Array.getLength(array);
        for (int i = 0; i < length; i++) {
            Object value = Array.get(array, i);
            if (value == null) continue;
            if (value.getClass().isArray()) {
                tuneWeaponArray(value, profile, index, visited);
                continue;
            }
            if (visited.put(value, Boolean.TRUE) != null) {
                continue;
            }
            String key = "weapon_" + index[0]++;
            int currentMax = readIntField(value, "maxAmmo");
            boolean firstProfile = !profile.contains(key, net.minecraft.nbt.Tag.TAG_INT);
            int base = firstProfile ? Math.max(1, currentMax) : Math.max(1, profile.getInt(key));
            if (firstProfile || profile.getInt(key) <= 0) {
                profile.putInt(key, base);
            }
            int desired = scaleAmmo(base);
            if (desired > 0) {
                writeIntField(value, "maxAmmo", desired);
                if (firstProfile) {
                    int current = Math.max(0, readIntField(value, "currentAmmo"));
                    writeIntField(value, "currentAmmo", Math.min(desired, scaleAmmo(current)));
                }
                // Replenish only the reserve. Keeping currentAmmo untouched after
                // initial scaling preserves the real magazine/reload cycle while
                // allowing reloads without any physical ammunition items.
                writeIntField(value, "backupAmmo", desired);
            }

            String fireTimeKey = key + "_fire_time";
            int currentFireTime = readIntField(value, "fireTime");
            if (currentFireTime > 0 || profile.contains(fireTimeKey, net.minecraft.nbt.Tag.TAG_INT)) {
                int baseFireTime = profile.contains(fireTimeKey, net.minecraft.nbt.Tag.TAG_INT)
                        ? profile.getInt(fireTimeKey) : currentFireTime;
                profile.putInt(fireTimeKey, baseFireTime);
                writeIntField(value, "fireTime", scaledDuration(baseFireTime, COMMANDER_WEAPON_SPEED));
            }
        }
    }

    private static int readIntField(Object object, String name) {
        try {
            Field field = object.getClass().getField(name);
            return field.getType() == int.class ? field.getInt(object) : 0;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return 0;
        }
    }

    private static boolean writeIntField(Object object, String name, int value) {
        try {
            Field field = object.getClass().getField(name);
            if (field.getType() != int.class) {
                return false;
            }
            field.setInt(object, value);
            return true;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return false;
        }
    }

    private static boolean writeBooleanField(Object object, String name, boolean value) {
        Class<?> type = object.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField(name);
                if (field.getType() != boolean.class || !field.trySetAccessible()) {
                    return false;
                }
                field.setBoolean(object, value);
                return true;
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
        return false;
    }

    private static Object readField(Object object, String name) {
        try {
            Field field = object.getClass().getField(name);
            return field.get(object);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Object invokeNoArg(Object object, String name) {
        try {
            return object.getClass().getMethod(name).invoke(object);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static void invokeMapSetter(Object object, String name, Map<?, ?> value) {
        try {
            object.getClass().getMethod(name, Map.class).invoke(object, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void clearHeat(Entity entity) {
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Field field : VEHICLE_HEAT_DATA.computeIfAbsent(entity.getClass(),
                type -> discoverDataAccessors(type, VEHICLE_HEAT_ACCESSORS))) {
            try {
                Object accessor = field.get(null);
                if (accessor != null && visited.add(accessor)) {
                    writeSyncedInt(entity, accessor, 0);
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
    }

    private static void refillVehicleAmmoData(Entity entity, CompoundTag persistent) {
        CompoundTag profile = persistent.getCompound(AMMO_DATA_PROFILE);
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Field field : VEHICLE_AMMO_DATA.computeIfAbsent(entity.getClass(),
                type -> discoverDataAccessors(type, VEHICLE_AMMO_ACCESSORS))) {
            try {
                Object accessor = field.get(null);
                if (accessor == null || !visited.add(accessor)) {
                    continue;
                }
                Integer current = readSyncedInt(entity, accessor);
                if (current == null) {
                    continue;
                }
                String key = field.getDeclaringClass().getSimpleName() + "_" + field.getName();
                int base = profile.contains(key, net.minecraft.nbt.Tag.TAG_INT)
                        ? Math.max(1, profile.getInt(key)) : Math.max(1, current);
                if (!profile.contains(key, net.minecraft.nbt.Tag.TAG_INT) || profile.getInt(key) <= 0) {
                    profile.putInt(key, base);
                }
                int desired = scaleAmmo(base);
                if (desired > 0) {
                    writeSyncedInt(entity, accessor, desired);
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
        persistent.put(AMMO_DATA_PROFILE, profile);
    }

    private static int scaleAmmo(int base) {
        return base > Integer.MAX_VALUE / 10 ? Integer.MAX_VALUE : Math.max(0, base * 10);
    }

    private static void accelerateVehicleTimers(Entity entity, double fireRate, double reloadRate) {
        if (positiveMultiplier(fireRate) <= 1.0001D && positiveMultiplier(reloadRate) <= 1.0001D) {
            return;
        }
        for (Field field : VEHICLE_INSTANCE_TIMERS.computeIfAbsent(entity.getClass(),
                SuperbWarfareCompat::discoverInstanceTimers)) {
            try {
                int extraTicks = timerExtraTicks(field.getName(), fireRate, reloadRate);
                int current = field.getInt(entity);
                if (extraTicks > 0 && current > 0) {
                    field.setInt(entity, Math.max(0, current - extraTicks));
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
        Set<Object> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Field field : VEHICLE_DATA_TIMERS.computeIfAbsent(entity.getClass(),
                type -> discoverDataAccessors(type, VEHICLE_TIMER_ACCESSORS))) {
            try {
                Object accessor = field.get(null);
                if (accessor == null || !visited.add(accessor)) {
                    continue;
                }
                int extraTicks = timerExtraTicks(field.getName(), fireRate, reloadRate);
                Integer current = readSyncedInt(entity, accessor);
                if (extraTicks > 0 && current != null && current > 0) {
                    writeSyncedInt(entity, accessor, Math.max(0, current - extraTicks));
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
    }

    private static int timerExtraTicks(String fieldName, double fireRate, double reloadRate) {
        String normalized = fieldName.toLowerCase(java.util.Locale.ROOT);
        double multiplier = normalized.contains("reload")
                ? reloadRate
                : normalized.contains("fire") || normalized.contains("shoot") || normalized.equals("cooldown")
                ? fireRate
                : Math.max(fireRate, reloadRate);
        return extraTicks(multiplier);
    }

    private static List<Field> discoverInstanceTimers(Class<?> initial) {
        java.util.ArrayList<Field> result = new java.util.ArrayList<>();
        for (Class<?> type = initial; type != null && type != Entity.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (field.getType() != int.class || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String name = field.getName().toLowerCase(java.util.Locale.ROOT);
                boolean timer = name.equals("cooldown")
                        || name.equals("handbusytime")
                        || name.contains("reloadcooldown")
                        || name.contains("firecooldown");
                if (timer && field.trySetAccessible()) {
                    result.add(field);
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<Field> discoverDataAccessors(Class<?> initial, Set<String> names) {
        java.util.ArrayList<Field> result = new java.util.ArrayList<>();
        for (Class<?> type = initial; type != null && type != Entity.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && names.contains(field.getName())
                        && field.getType().getName().equals("net.minecraft.network.syncher.EntityDataAccessor")
                        && field.trySetAccessible()) {
                    result.add(field);
                }
            }
        }
        return List.copyOf(result);
    }

    @SuppressWarnings("unchecked")
    private static Integer readSyncedInt(Entity entity, Object rawAccessor) {
        try {
            net.minecraft.network.syncher.EntityDataAccessor<Object> accessor =
                    (net.minecraft.network.syncher.EntityDataAccessor<Object>) rawAccessor;
            Object value = entity.getEntityData().get(accessor);
            return value instanceof Integer integer ? integer : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static void writeSyncedInt(Entity entity, Object rawAccessor, int value) {
        try {
            net.minecraft.network.syncher.EntityDataAccessor<Object> accessor =
                    (net.minecraft.network.syncher.EntityDataAccessor<Object>) rawAccessor;
            entity.getEntityData().set(accessor, value);
        } catch (RuntimeException ignored) {
        }
    }

    private static void applyGunDataModifiers(Player player, ItemStack stack, double fireRate, double reloadRate,
                                              double aimRate, double boltRate, boolean disabled) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        try {
            Class<?> gunItemClass = Class.forName("com.atsuishio.superbwarfare.item.gun.GunItem");
            if (!gunItemClass.isInstance(stack.getItem())) {
                return;
            }
            Class<?> gunDataClass = Class.forName("com.atsuishio.superbwarfare.data.gun.GunData");
            Object gunData = gunDataClass.getMethod("from", ItemStack.class).invoke(null, stack);
            GunModifierState state = new GunModifierState();
            state.fireRate = positiveMultiplier(fireRate);
            state.reloadRate = positiveMultiplier(reloadRate);
            state.aimRate = positiveMultiplier(aimRate);
            state.boltRate = positiveMultiplier(boltRate);
            applyGunPropertyModifiers(gunData, state);
            reduceGunTimers(gunData, state);
            updateGunDisabledState(player, stack, gunData, disabled);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // SuperbWarfare is optional and its gun API differs between releases.
        }
    }

    private static void applyGunPropertyModifiers(Object gunData, GunModifierState state) {
        for (String property : List.of(
                "RPM", "SHOOT_DELAY", "NORMAL_RELOAD_TIME", "EMPTY_RELOAD_TIME",
                "PREPARE_TIME", "PREPARE_LOAD_TIME", "PREPARE_AMMO_LOAD_TIME",
                "PREPARE_EMPTY_TIME", "ITERATIVE_TIME", "ITERATIVE_AMMO_LOAD_TIME",
                "FINISH_TIME", "BOLT_ACTION_TIME")) {
            try {
                setGunPropertyModifier(gunData, property, state);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Bind every property independently so a renamed optional property
                // cannot disable the remaining SuperbWarfare properties.
            }
        }
    }

    private static void setGunPropertyModifier(Object gunData, String propertyName, GunModifierState state)
            throws ReflectiveOperationException {
        Class<?> propClass = Class.forName("com.atsuishio.superbwarfare.data.gun.GunProp");
        Class<?> contextClass = Class.forName("com.atsuishio.superbwarfare.data.Prop$PropModifyContext");
        Object property = propClass.getField(propertyName).get(null);
        Object context = Proxy.newProxyInstance(contextClass.getClassLoader(), new Class<?>[] {contextClass},
                (proxy, method, args) -> {
                    if ("apply".equals(method.getName()) && args != null && args.length == 3
                            && args[2] instanceof Number number) {
                        return state.modify(propertyName, number.intValue());
                    }
                    if ("toString".equals(method.getName())) return "DFS SuperbWarfare " + propertyName;
                    if ("hashCode".equals(method.getName())) return System.identityHashCode(proxy);
                    if ("equals".equals(method.getName())) return args != null && args.length == 1 && proxy == args[0];
                    return null;
                });
        gunData.getClass().getMethod("setTempProperty", propClass, contextClass)
                .invoke(gunData, property, context);
    }

    private static void reduceGunTimers(Object gunData, GunModifierState state) {
        reducePublicTimers(readField(gunData, "reload"), extraTicks(state.reloadRate));
        reducePublicTimers(readField(gunData, "bolt"), extraTicks(state.boltRate));
        reducePublicTimers(readField(gunData, "charge"),
                extraTicks(Math.max(state.fireRate, state.aimRate)));
    }

    private static void reducePublicTimers(Object owner, int extraTicks) {
        if (owner == null || extraTicks <= 0) {
            return;
        }
        for (Field field : owner.getClass().getFields()) {
            if (!field.getType().getName().equals("com.atsuishio.superbwarfare.data.gun.value.Timer")) {
                continue;
            }
            try {
                Object timer = field.get(owner);
                int current = ((Number) timer.getClass().getMethod("get").invoke(timer)).intValue();
                if (current > 0) {
                    timer.getClass().getMethod("set", int.class).invoke(timer, Math.max(0, current - extraTicks));
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
            }
        }
    }

    private static void updateGunDisabledState(Player player, ItemStack stack, Object gunData, boolean disabled) {
        CompoundTag stackTag = stack.getTag();
        boolean forced = stackTag != null && stackTag.getBoolean(FORCED_GUN_STOP);
        if (forced) {
            setGunBooleanValue(gunData, "stopped", false);
            setGunBooleanValue(gunData, "forceStop", false);
            stack.getOrCreateTag().remove(FORCED_GUN_STOP);
        }
        if (disabled) {
            player.getCooldowns().addCooldown(stack.getItem(), 2);
            player.stopUsingItem();
        }
    }

    private static void setGunBooleanValue(Object gunData, String fieldName, boolean value) {
        try {
            Object holder = gunData.getClass().getField(fieldName).get(gunData);
            holder.getClass().getMethod("set", boolean.class).invoke(holder, value);
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void setGunNumberValue(Object gunData, String fieldName, Number value) {
        try {
            Object holder = gunData.getClass().getField(fieldName).get(gunData);
            for (Method method : holder.getClass().getMethods()) {
                if (!"set".equals(method.getName()) || method.getParameterCount() != 1) {
                    continue;
                }
                Class<?> parameter = method.getParameterTypes()[0];
                Object converted;
                if (parameter == int.class || parameter == Integer.class) converted = value.intValue();
                else if (parameter == long.class || parameter == Long.class) converted = value.longValue();
                else if (parameter == float.class || parameter == Float.class) converted = value.floatValue();
                else if (parameter == double.class || parameter == Double.class) converted = value.doubleValue();
                else continue;
                method.invoke(holder, converted);
                return;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
        }
    }

    private static void adjustClientAim(ItemStack stack, double aimRate, double aimPenalty) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        try {
            Class<?> gunItemClass = Class.forName("com.atsuishio.superbwarfare.item.gun.GunItem");
            if (!gunItemClass.isInstance(stack.getItem())) {
                return;
            }
            Class<?> handler = clientEventHandlerClass();
            if (handler == null || !handler.getField("zoom").getBoolean(null)) {
                return;
            }
            Field zoomTime = handler.getField("zoomTime");
            double current = zoomTime.getDouble(null);
            double boost = Math.min(0.5D, Math.max(0.0D, 0.08D * (positiveMultiplier(aimRate) - 1.0D)));
            double penalty = Math.min(0.8D, Math.max(0.0D, 0.08D * (positiveMultiplier(aimPenalty) - 1.0D)));
            zoomTime.setDouble(null, Math.max(0.0D, Math.min(1.0D, current + boost - penalty)));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // SuperbWarfare is optional and moved its client event class between releases.
        }
    }

    private static void adjustClientVehicleTimer(double fireRate) {
        try {
            Class<?> handler = clientEventHandlerClass();
            if (handler == null) {
                return;
            }
            Object timer = handler.getField("clientTimerVehicle").get(null);
            if (timer == null || !((Boolean) timer.getClass().getMethod("started").invoke(timer))) {
                return;
            }
            long current = ((Number) timer.getClass().getMethod("getProgress").invoke(timer)).longValue();
            long adjustment = Math.round(50.0D * (positiveMultiplier(fireRate) - 1.0D));
            timer.getClass().getMethod("setProgress", long.class)
                    .invoke(timer, Math.max(0L, current + adjustment));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Client-only timer fields differ between optional SuperbWarfare versions.
        }
    }

    private static Class<?> clientEventHandlerClass() {
        for (String name : List.of(
                "com.atsuishio.superbwarfare.event.ClientEventHandler",
                "com.atsuishio.superbwarfare.client.ClientEventHandler")) {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException | LinkageError ignored) {
            }
        }
        return null;
    }

    private static int extraTicks(double multiplier) {
        return Math.max(0, (int) Math.ceil(positiveMultiplier(multiplier)) - 1);
    }

    private static double positiveMultiplier(double multiplier) {
        return Double.isFinite(multiplier) && multiplier > 0.0D ? multiplier : 1.0D;
    }

    private static final class GunModifierState {
        private volatile double fireRate = 1.0D;
        private volatile double reloadRate = 1.0D;
        private volatile double aimRate = 1.0D;
        private volatile double boltRate = 1.0D;

        private int modify(String propertyName, int value) {
            double multiplier;
            if ("RPM".equals(propertyName)) {
                return scaledUp(value, fireRate);
            }
            if ("SHOOT_DELAY".equals(propertyName)) {
                multiplier = fireRate;
            } else if ("BOLT_ACTION_TIME".equals(propertyName)) {
                multiplier = boltRate;
            } else {
                multiplier = reloadRate;
            }
            return scaledDuration(value, multiplier);
        }

        private static int scaledUp(int value, double multiplier) {
            return (int) Math.min(Integer.MAX_VALUE, Math.max(1.0D, Math.ceil(value * multiplier)));
        }

        private static int scaledDuration(int value, double multiplier) {
            return SuperbWarfareCompat.scaledDuration(value, multiplier);
        }
    }

    private static int scaledDuration(int value, double multiplier) {
        return value <= 0 ? value : Math.max(1, (int) Math.ceil(value / positiveMultiplier(multiplier)));
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    private static Class<?> vehicleClass() {
        if (!isLoaded()) {
            return null;
        }
        if (!vehicleClassResolved) {
            synchronized (SuperbWarfareCompat.class) {
                if (!vehicleClassResolved) {
                    try {
                        vehicleClass = Class.forName(VEHICLE_ENTITY_CLASS);
                        getMaxHealthMethod = vehicleClass.getMethod("getMaxHealth");
                    } catch (ReflectiveOperationException | LinkageError ignored) {
                        vehicleClass = null;
                        getMaxHealthMethod = null;
                    }
                    vehicleClassResolved = true;
                }
            }
        }
        return vehicleClass;
    }

    private static float maxHealth(Entity entity) {
        Method method = getMaxHealthMethod;
        if (method == null || !isVehicle(entity)) {
            return 0.0F;
        }
        try {
            Object value = method.invoke(entity);
            return value instanceof Number number ? number.floatValue() : 0.0F;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return 0.0F;
        }
    }

    private static Vec3 closestPoint(Vec3 point, AABB bounds) {
        double x = Math.max(bounds.minX, Math.min(point.x, bounds.maxX));
        double y = Math.max(bounds.minY, Math.min(point.y, bounds.maxY));
        double z = Math.max(bounds.minZ, Math.min(point.z, bounds.maxZ));
        return new Vec3(x, y, z);
    }

    private static boolean hasLineOfSight(ServerLevel level, Vec3 center, Vec3 target, Entity context) {
        if (center.distanceToSqr(target) < 1.0E-4D) {
            return true;
        }
        HitResult result = level.clip(new ClipContext(center, target, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, context));
        return result.getType() == HitResult.Type.MISS || result.getLocation().distanceToSqr(target) < 0.25D;
    }
}
