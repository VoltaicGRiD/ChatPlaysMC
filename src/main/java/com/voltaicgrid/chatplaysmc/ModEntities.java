package com.voltaicgrid.chatplaysmc;

import net.minecraft.entity.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

public final class ModEntities {
    private ModEntities() {}

    public static final Identifier ARITSUKI_ID = Identifier.of(ChatPlaysMcMod.MOD_ID, "aritsuki");
    public static final RegistryKey<EntityType<?>> ARITSUKI_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, ARITSUKI_ID);

    public static final EntityType<AritsukiEntity> ARITSUKI = Registry.register(
            Registries.ENTITY_TYPE,
            ARITSUKI_KEY,
            EntityType.Builder.<AritsukiEntity>create(AritsukiEntity::new, SpawnGroup.MISC)
                    .dimensions(0.6f, 1.8f)
                    .maxTrackingRange(80)
                    .build(ARITSUKI_KEY)
    );

    /** Call from your ModInitializer#onInitialize */
    public static void init() {
        // Required for LivingEntity types (will crash without default attributes)
        FabricDefaultAttributeRegistry.register(ARITSUKI, AritsukiEntity.createMobAttributes());
    }
}