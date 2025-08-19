package com.voltaicgrid.chatplaysmc;

import net.minecraft.item.*;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

public class SoapItem extends Item {
	public SoapItem(Settings settings) {
		super(settings);
	}
	
	@Override
	public ItemStack finishUsing(ItemStack item, World world, LivingEntity user) {
//		BlockPos blockPos = user.getBlockPos().offset(0, -1, 0);
//		Server server = world.getServer();
//		
//		server.execute(() -> {
//			user.getServerWorld().setBlockState(blockPos, Blocks..getDefaultState());
//		});
		
		super.finishUsing(item, world, user);
		return new ItemStack(ModItems.BittenSoap);
	}
}
