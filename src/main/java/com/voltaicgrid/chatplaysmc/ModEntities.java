package com.voltaicgrid.chatplaysmc;

import net.minecraft.entity.*;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class ModEntities {
    private ModEntities() {}

    // Public access to the type for spawning and renderer registration
    public static EntityType<GrapplingHookEntity> GRAPPLING_HOOK;

    public static void register() {
        // Basic small projectile-like entity (snowball-sized)
        GRAPPLING_HOOK = Registry.register(
            Registries.ENTITY_TYPE,
            Identifier.of(ChatPlaysMcMod.MOD_ID, "grappling_hook"),
            EntityType.Builder.<GrapplingHookEntity>create(GrapplingHookEntity::new, SpawnGroup.MISC).dimensions(0.75f, 0.75f).build(RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(ChatPlaysMcMod.MOD_ID, "grappling_hook")))
        );
    }
}