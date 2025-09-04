package com.voltaicgrid.chatplaysmc;

import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.WindChargeEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

public class AritsukiShieldItem extends ShieldItem {
    private static final int COOLDOWN_TICKS = 5 * 20; // 5 seconds

    public AritsukiShieldItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.BLOCK;
    }

    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        // Begin using (this is what actually raises the shield client-side)
        user.setCurrentHand(hand);


        return ActionResult.CONSUME;
    }

    public boolean onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (world.isClient || !(user instanceof PlayerEntity player)) return false;
        if (player.getItemCooldownManager().isCoolingDown(stack)) return false;

        if (!world.isClient && world instanceof ServerWorld serverWorld) {
            spawnWindRing(serverWorld, player, 30);
            player.getItemCooldownManager().set(Identifier.of("chat_plays_mc", "aritsuki_shield"), COOLDOWN_TICKS);
        }
        
        return true;
    }
    
    private void spawnWindRing(ServerWorld world, PlayerEntity user, int stepDegrees) {
        final double y = user.getY() + 3.0; // chest height; adjust to taste
        
        // Spiral parameters
        final double startRadius = 3.0;
        final double endRadius = 5.0;
        final int totalRotations = 2; // Number of full rotations in the spiral
        final int totalDegrees = 360 * totalRotations;
        
        final int DELAY_BETWEEN_CHARGES = 1; // 5 ticks = quarter second
        
        // Create a list of spawn positions and schedule them
        int chargeIndex = 0;
        for (int deg = 0; deg < totalDegrees; deg += stepDegrees) {
            // Calculate radius that expands as we go around the spiral
            double progress = (double) deg / totalDegrees; // 0.0 to 1.0
            double radius = startRadius + (endRadius - startRadius) * progress;
            
            double rad = Math.toRadians(deg);
            double x = user.getX() + Math.cos(rad) * radius;
            double z = user.getZ() + Math.sin(rad) * radius;

            ChatPlaysMcMod.SCHEDULER.runLater(world.getServer(), chargeIndex * DELAY_BETWEEN_CHARGES, () -> {
    			spawnSingleWindCharge(world, user, x, y, z);
    		});            
            chargeIndex++;
        }
    }
    
    private void spawnSingleWindCharge(ServerWorld world, PlayerEntity user, double x, double y, double z) {
        WindChargeEntity charge = EntityType.WIND_CHARGE.create(world, SpawnReason.TRIGGERED);
        if (charge == null) return;

        charge.setOwner(user);
        charge.refreshPositionAndAngles(x, y, z, 0.0F, 0.0F);
        charge.setVelocity(0.0, -1.0, 0.0);
        world.spawnEntity(charge);
    }
}