package com.voltaicgrid.chatplaysmc;

import net.minecraft.item.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.item.tooltip.*;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.BlockPos;
import java.util.function.Consumer;
import net.minecraft.nbt.NbtCompound;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TorchSatchelItem extends Item {
	// Use a static map to track cooldowns per item instance, avoiding constant NBT updates
	private static final Map<UUID, Integer> cooldownMap = new HashMap<>();
	
	public TorchSatchelItem(Settings settings) {
		super(settings);
	}
	
	private UUID getStackId(ItemStack stack) {
		// Create a unique identifier for this specific item stack
		var customData = stack.get(DataComponentTypes.CUSTOM_DATA);
		if (customData != null) {
			var nbt = customData.copyNbt();
			if (nbt.contains("stackId")) {
				return UUID.fromString(nbt.getString("stackId").orElse(null));
			}
		}
		
		// If no ID exists, create one and store it
		UUID newId = UUID.randomUUID();
		NbtCompound nbt = new NbtCompound();
		nbt.putString("stackId", newId.toString());
		stack.set(DataComponentTypes.CUSTOM_DATA, net.minecraft.component.type.NbtComponent.of(nbt));
		return newId;
	}
	
	@Override
	public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
		if (!(entity instanceof PlayerEntity player)) {
			return;
		}
		
		UUID stackId = getStackId(stack);
		int ticksSincePlace = cooldownMap.getOrDefault(stackId, 3 * 20); // Default to ready
		
		int lightLevel = world.getLightLevel(entity.getBlockPos());
		
		if (lightLevel < ChatPlaysMcMod.CONFIG.torchSatchelLightThreshold()) {
			BlockPos pos = entity.getBlockPos();
			BlockPos belowPos = pos.down();
			
			// Check if current position and above are air, and there's a solid block below
			if (world.getBlockState(pos).isAir() && 
				world.getBlockState(pos.up()).isAir() && 
				world.getBlockState(belowPos).isSolidBlock(world, belowPos) &&
				ticksSincePlace >= 3 * 20) {
				// Place a torch
				world.setBlockState(pos, Blocks.TORCH.getDefaultState());
				world.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.PLAYERS, 1.0F, 1.0F);	
				
				// Reset cooldown in memory
				cooldownMap.put(stackId, 0);
			} else {
				// Increment cooldown
				cooldownMap.put(stackId, ticksSincePlace + 1);
			}
		} else {
			// Increment cooldown
			cooldownMap.put(stackId, ticksSincePlace + 1);
		}
		
		super.inventoryTick(stack, world, entity, slot);
	}
	
	@Override
	public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent tooltipDisplay, Consumer<Text> callback, TooltipType type) {
		super.appendTooltip(stack, context, tooltipDisplay, callback, type);
		
		ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
		if (container != null) {
			int foodCount = 0;
			
			for (ItemStack itemStack : container.iterateNonEmpty()) {	
				foodCount += itemStack.getCount();
			}
			
			callback.accept(Text.literal("Food items: " + foodCount));
			callback.accept(Text.literal("Sneak + Right-click to view contents"));
		}
	}
	
	@Override
	public boolean isItemBarVisible(ItemStack stack) {
		// Show the cooldown bar when the item is on cooldown
		UUID stackId = getStackId(stack);
		int ticksSincePlace = cooldownMap.getOrDefault(stackId, 3 * 20);
		return ticksSincePlace < 3 * 20; // Show bar during 3 second cooldown
	}
	
	@Override
	public int getItemBarStep(ItemStack stack) {
		// Calculate the progress of the cooldown (0-13 scale for the bar)
		UUID stackId = getStackId(stack);
		int ticksSincePlace = cooldownMap.getOrDefault(stackId, 3 * 20);
		int maxCooldown = 3 * 20; // 3 seconds
		
		// Return progress from 0 (just used) to 13 (ready to use)
		return Math.min(13, (ticksSincePlace * 13) / maxCooldown);
	}
	
	@Override
	public int getItemBarColor(ItemStack stack) {
		// Use a yellow/orange color for the cooldown bar to match torch theme
		return 0xFFA500; // Orange color
	}
}