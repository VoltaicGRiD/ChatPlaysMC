package com.voltaicgrid.chatplaysmc.mixin;

import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityDataTrackerMixin {
    // No-op; lock-on state is managed client-side without DataTracker.
}