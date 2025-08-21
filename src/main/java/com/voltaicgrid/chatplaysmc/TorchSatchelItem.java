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
import java.util.List;

public class TorchSatchelItem extends Item {
	public TorchSatchelItem(Settings settings) {
		super(settings);
	}
	
	@Override
	public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
		if (!(entity instanceof PlayerEntity player)) {
			return;
		}
		
		int lightLevel = world.getLightLevel(entity.getBlockPos());
		
		if (lightLevel < ChatPlaysMcMod.CONFIG.torchSatchelLightThreshold()) {
			BlockPos pos = entity.getBlockPos();
			BlockPos belowPos = pos.down();
			
			// Check if current position and above are air, and there's a solid block below
			if (world.getBlockState(pos).isAir() && 
				world.getBlockState(pos.up()).isAir() && 
				world.getBlockState(belowPos).isSolidBlock(world, belowPos)) {
				// Place a torch if the player is sneaking
				world.setBlockState(pos, Blocks.TORCH.getDefaultState());
				world.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.PLAYERS, 1.0F, 1.0F);	
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
}