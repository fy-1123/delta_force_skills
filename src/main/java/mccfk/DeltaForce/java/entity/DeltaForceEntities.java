package mccfk.DeltaForce.java.entity;

import mccfk.DeltaForce.java.DeltaForce;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * DeltaForce实体注册
 */
public class DeltaForceEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, DeltaForce.MODID);

    public static final RegistryObject<EntityType<GroundItemEntity>> GROUND_ITEM =
        ENTITY_TYPES.register("df_item_e", () -> EntityType.Builder.<GroundItemEntity>of(
                GroundItemEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.25F)
            .clientTrackingRange(6)
            .updateInterval(20)
            .build("df_item_e"));
}
