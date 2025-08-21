package com.voltaicgrid.chatplaysmc;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.*;
import net.minecraft.item.*;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Direction;
import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;

public class ShaftBuilder extends Item {
	public ShaftBuilder(Settings settings) {
		super(settings);
	}
	
	@Override
	public ActionResult useOnBlock(ItemUsageContext context) {
		BlockPos pos = context.getBlockPos();
		World world = context.getWorld();
		// Only run logic on the server to avoid ClientWorld -> ServerWorld cast
		if (!(world instanceof ServerWorld serverWorld)) {
			return ActionResult.PASS;
		}
		Direction side = context.getSide();
		var blockStacks = new ArrayList<ItemStack>();
		Direction playerFacing = context.getPlayer().getHorizontalFacing();
		
		for (int depth = 0; depth < ChatPlaysMcMod.CONFIG.shaftBuilderDepth(); depth++) {
			for (int height = 0; height < 4; height++) {
				BlockPos targetPos = pos.down(depth).offset(playerFacing, depth).offset(playerFacing, height);
				BlockState targetState = serverWorld.getBlockState(targetPos);
				if (!targetState.isAir() && targetState.getBlock() != Blocks.BEDROCK && targetState.getBlock() != Blocks.OBSIDIAN) {
					// Collect drops before removing the block
					for (ItemStack stack : Block.getDroppedStacks(targetState, serverWorld, targetPos, serverWorld.getBlockEntity(targetPos))) {
						blockStacks.add(stack);
					}
					// Remove the block (create the shaft)
					serverWorld.setBlockState(targetPos, Blocks.AIR.getDefaultState());
					// Play sound effect for breaking the block
					serverWorld.playSound(null, targetPos, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
				}
			}
		}
		
		for (ItemStack stack : blockStacks) {
			if (!stack.isEmpty()) {
				// Drop the collected blocks as items at the original position
				serverWorld.spawnEntity(new ItemEntity(serverWorld, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, stack));
			}
		}
		
		// Consume the item
		context.getStack().decrement(1);
		
		return ActionResult.SUCCESS;
	}
}