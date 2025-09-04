package com.voltaicgrid.chatplaysmc;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.math.Vec3d;

public class GrapplingHookItem extends Item {
	public GrapplingHookItem(Settings settings) {
		super(settings);
	}
	
	@Override 
	public ActionResult use(World world, PlayerEntity player, Hand hand) {
		ItemStack itemStack = player.getStackInHand(hand);
		
		if (!world.isClient) {
			// Create and throw the grappling hook entity
			GrapplingHookEntity grapplingHook = new GrapplingHookEntity(world, player);
			
			// Set the velocity based on player's look direction
			Vec3d lookDirection = player.getRotationVec(1.0F);
			grapplingHook.setVelocity(lookDirection.multiply(1.5)); // Throw speed
			
			// Position the hook slightly in front of the player
			Vec3d startPos = player.getCameraPosVec(1.0F).add(lookDirection.multiply(1.0));
			grapplingHook.setPosition(startPos.x, startPos.y, startPos.z);
			
			// Spawn the entity
			world.spawnEntity(grapplingHook);
			
			// Play throwing sound
			world.playSound(null, player.getX(), player.getY(), player.getZ(), 
				SoundEvents.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.0F, 1.0F);
		}
		
		// Update player stats
		player.incrementStat(Stats.USED.getOrCreateStat(this));
		
		// Don't consume the item (reusable like fishing rod)
		return ActionResult.SUCCESS;
	}
}