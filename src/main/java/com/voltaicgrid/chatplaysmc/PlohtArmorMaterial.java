package com.voltaicgrid.chatplaysmc;

import net.minecraft.item.*;

import java.util.Map;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.item.equipment.*;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.*;

public class PlohtArmorMaterial {
	public static final int BASE_DURABILITY = 1; // Very fragile - breaks after 1-2 hits
	public static final RegistryKey<EquipmentAsset> PLOHT_ARMOR_MATERIAL_KEY = RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of(ChatPlaysMcMod.MOD_ID, "ploht"));
	
	public static final ArmorMaterial INSTANCE = new ArmorMaterial(
			BASE_DURABILITY,
			Map.of(
					EquipmentType.HELMET, 20,        // Maximum protection values
					EquipmentType.CHESTPLATE, 32,    // Higher than diamond/netherite
					EquipmentType.LEGGINGS, 28,
					EquipmentType.BOOTS, 16
			),
			30, // Very high enchantability - easier to get good enchants
			SoundEvents.ITEM_ARMOR_EQUIP_IRON, // Glass breaking sound for thematic effect
			20.0F, // Very high toughness - reduces damage from strong attacks
			1.0F,  // Full knockback resistance
			null,
			PLOHT_ARMOR_MATERIAL_KEY
	);
}