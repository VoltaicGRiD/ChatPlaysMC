package com.voltaicgrid.chatplaysmc;

import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;

public class MagnetItem extends Item {
	private static final int RADIUS = 5; // Define the radius to search for entities
	
	public MagnetItem(Settings settings) {
		super(settings);
	}
	
	@Override
	public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
		if (!world.isClient) {
			List<ItemEntity> nearbyItems = getEntitiesNearby(entity, world);
			for (ItemEntity itemEntity : nearbyItems) {
				if (itemEntity.cannotPickup()) continue; // Skip if the item cannot be picked up
				if (itemEntity.getOwner() != null && itemEntity.getOwner().equals(entity.getUuid())) continue; // Skip if owned by someone else
				itemEntity.setPosition(entity.getX(), entity.getY(), entity.getZ());
			}
		}
	}

	private List<ItemEntity> getEntitiesNearby(Entity entity, World world) {
		return world.getOtherEntities(entity, entity.getBoundingBox().expand(RADIUS), e -> e instanceof ItemEntity)
				.stream()
				.map(e -> (ItemEntity) e)
				.toList();
	}
}