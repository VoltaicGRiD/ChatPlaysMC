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
import net.minecraft.sound.*;
import java.util.function.Consumer;
import java.util.List;

public class FoodSatchelItem extends Item {
	public FoodSatchelItem(Settings settings) {
		super(settings);
	}
	
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		if (context.getWorld().isClient) {
			return ActionResult.SUCCESS;
		}
		
		PlayerEntity player = context.getPlayer();
		if (player != null) {
			// Check to make sure the stack is a food
			ItemStack mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
			ItemStack offHandStack = player.getStackInHand(Hand.OFF_HAND);
			
			if (isValidFood(mainHandStack)) {
				if (addFoodToSatchel(context.getStack(), mainHandStack)) {
					mainHandStack.setCount(mainHandStack.getCount() - 1);
					player.sendMessage(Text.literal("Added " + mainHandStack.getName().getString() + " to Food Satchel"), true);
					return ActionResult.SUCCESS;
				}
			} else if (isValidFood(offHandStack)) {
				if (addFoodToSatchel(context.getStack(), offHandStack)) {
					offHandStack.setCount(offHandStack.getCount() - 1);
					player.sendMessage(Text.literal("Added " + offHandStack.getName().getString() + " to Food Satchel"), true);
					return ActionResult.SUCCESS;
				}
			}
			
			if (offHandStack.isEmpty()) {
				// If the off-hand is empty, and the satchel is in the main hand, drop the satchel contents			
				ContainerComponent container = mainHandStack.get(DataComponentTypes.CONTAINER);
				if (container == null) {
					return ActionResult.PASS;
				}
				
				DefaultedList<ItemStack> items = DefaultedList.ofSize(3, ItemStack.EMPTY);
				int index = 0;
				for (ItemStack item : container.iterateNonEmpty()) {
					if (index < 3) {
						items.set(index, item.copy());
						index++;
					}
				}
				
				boolean droppedItems = false;
				for (int i = 0; i < items.size(); i++) {
					ItemStack itemStack = items.get(i);
					if (!itemStack.isEmpty()) {
						// Drop the item in the world
						player.dropItem(itemStack, false);
						// Clear the stack in our copy
						items.set(i, ItemStack.EMPTY);
						droppedItems = true;
					}
				}
				
				// Update the container component to reflect the dropped items
				if (droppedItems) {
					ContainerComponent newContainer = ContainerComponent.fromStacks(items);
					mainHandStack.set(DataComponentTypes.CONTAINER, newContainer);
					player.sendMessage(Text.literal("Dropped all items from Food Satchel"), true);
				}
			}
			
			return ActionResult.SUCCESS;
		}
		
		return ActionResult.PASS;
	}
	
	@Override
	public ActionResult use(World world, PlayerEntity player, Hand hand) {	
		if (!world.isClient) {
			// Check to make sure the stack is a food
			ItemStack mainHandStack = player.getStackInHand(Hand.MAIN_HAND);
			ItemStack offHandStack = player.getStackInHand(Hand.OFF_HAND);
			
			if (isValidFood(mainHandStack)) {
				if (addFoodToSatchel(offHandStack, mainHandStack)) {
					mainHandStack.setCount(mainHandStack.getCount() - 1);
					player.sendMessage(Text.literal("Added " + mainHandStack.getName().getString() + " to Food Satchel"), true);
					return ActionResult.SUCCESS;
				}
			} else if (isValidFood(offHandStack)) {
				if (addFoodToSatchel(mainHandStack, offHandStack)) {
					offHandStack.setCount(offHandStack.getCount() - 1);
					player.sendMessage(Text.literal("Added " + offHandStack.getName().getString() + " to Food Satchel"), true);
					return ActionResult.SUCCESS;
				}
			}
			
			if (offHandStack.isEmpty()) {
				// If the off-hand is empty, and the satchel is in the main hand, drop the satchel contents			
				ContainerComponent container = mainHandStack.get(DataComponentTypes.CONTAINER);
				if (container == null) {
					return ActionResult.PASS;
				}
				
				DefaultedList<ItemStack> items = DefaultedList.ofSize(3, ItemStack.EMPTY);
				int index = 0;
				for (ItemStack item : container.iterateNonEmpty()) {
					if (index < 3) {
						items.set(index, item.copy());
						index++;
					}
				}
				
				boolean droppedItems = false;
				for (int i = 0; i < items.size(); i++) {
					ItemStack itemStack = items.get(i);
					if (!itemStack.isEmpty()) {
						// Drop the item in the world
						player.dropItem(itemStack, true);
						// Clear the stack in our copy
						items.set(i, ItemStack.EMPTY);
						droppedItems = true;
					}
				}
				
				// Update the container component to reflect the dropped items
				if (droppedItems) {
					ContainerComponent newContainer = ContainerComponent.fromStacks(items);
					mainHandStack.set(DataComponentTypes.CONTAINER, newContainer);
					player.sendMessage(Text.literal("Dropped all items from Food Satchel"), true);
				}
			}
			
			return ActionResult.SUCCESS;
		}
		
		return ActionResult.PASS;
	}

	private int findSatchelSlot(PlayerEntity player) {
		// Find the satchel in the player's inventory
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (stack.getItem() instanceof FoodSatchelItem) {
				return i;
			}
		}
		return -1; // Not found
	}
	
	@Override
	public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
		if (!(entity instanceof PlayerEntity player)) {
			return;
		}
		
		ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
		if (container == null) {
			return;
		}
		
		// Auto-feed from satchel when player is hungry
		if (player.getHungerManager().isNotFull() && world.getTime() % 100 == 0) { // Check every 5 seconds
			
			// Create a mutable copy of the container items
			DefaultedList<ItemStack> items = DefaultedList.ofSize(3, ItemStack.EMPTY);
			int index = 0;
			for (ItemStack item : container.iterateNonEmpty()) {
				if (index < 3) {
					items.set(index, item.copy());
					index++;
				}
			}
			
			// Look for food to consume
			boolean consumedFood = false;
			for (int i = 0; i < items.size(); i++) {
				ItemStack itemStack = items.get(i);
				if (!itemStack.isEmpty() && itemStack.getItem().getComponents().contains(DataComponentTypes.FOOD)) {
					FoodComponent food = itemStack.get(DataComponentTypes.FOOD);
					if (food != null) {
						// Consume the food
						player.getHungerManager().add(food.nutrition(), food.saturation());
						
						// Reduce the count
						itemStack.setCount(itemStack.getCount() - 1);
						
						// Play eating sound
						player.playSound(SoundEvents.ENTITY_PLAYER_BURP, 1.0f, 1.0f);
						
						// If the stack is empty, clear it
						if (itemStack.getCount() <= 0) {
							items.set(i, ItemStack.EMPTY);
						}
						
						player.sendMessage(Text.literal("Consumed " + itemStack.getName().getString() + " from Food Satchel"), true);
						consumedFood = true;
						break; // Only consume one item per tick
					}
				}
			}
			
			// Update the container component if food was consumed
			if (consumedFood) {
				ContainerComponent newContainer = ContainerComponent.fromStacks(items);
				stack.set(DataComponentTypes.CONTAINER, newContainer);
			}
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
	
	private boolean isValidFood(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem().getComponents().contains(DataComponentTypes.FOOD);
	}
	
	private boolean addFoodToSatchel(ItemStack satchelStack, ItemStack foodStack) {
		// Get or create container component
		ContainerComponent container = satchelStack.get(DataComponentTypes.CONTAINER);
		DefaultedList<ItemStack> items;
		
		if (container == null) {
			// Create a new container with 9 slots (like a bundle)
			items = DefaultedList.ofSize(3, ItemStack.EMPTY);
		} else {
			// Copy existing items
			items = DefaultedList.ofSize(3, ItemStack.EMPTY);
			int index = 0;
			for (ItemStack item : container.iterateNonEmpty()) {
				if (index < 3) {
					items.set(index, item.copy());
					index++;
				}
			}
		}
		
		// Only try to add 1 item instead of the whole stack
		int remainingCount = 1; // Changed from foodStack.getCount() to 1
		
		// Try to add the food item to existing stacks first
		for (int i = 0; i < items.size() && remainingCount > 0; i++) {
			ItemStack slotStack = items.get(i);
			if (!slotStack.isEmpty() && ItemStack.areItemsEqual(slotStack, foodStack)) {
				int maxStackSize = Math.min(slotStack.getMaxCount(), foodStack.getMaxCount());
				int canAdd = maxStackSize - slotStack.getCount();
				int toAdd = Math.min(canAdd, remainingCount);
				
				if (toAdd > 0) {
					slotStack.setCount(slotStack.getCount() + toAdd);
					remainingCount -= toAdd;
				}
			}
		}
		
		// If there's still food remaining, try to add to empty slots
		for (int i = 0; i < items.size() && remainingCount > 0; i++) {
			ItemStack slotStack = items.get(i);
			if (slotStack.isEmpty()) {
				ItemStack newStack = foodStack.copy();
				newStack.setCount(1); // Only add 1 item
				items.set(i, newStack);
				remainingCount -= 1;
			}
		}
				
		// Update the container component
		ContainerComponent newContainer = ContainerComponent.fromStacks(items);
		satchelStack.set(DataComponentTypes.CONTAINER, newContainer);
		
		// Return true if we successfully added the food item
		return remainingCount == 0;
	}
}