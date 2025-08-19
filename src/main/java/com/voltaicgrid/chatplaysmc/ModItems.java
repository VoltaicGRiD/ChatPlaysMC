package com.voltaicgrid.chatplaysmc;

import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.util.*;
import net.minecraft.component.type.*;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.entity.effect.*;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.*;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.function.Function;

public class ModItems {
	public static void initialize() {
	}
	
	public static Item register(String name, Function<Item.Settings, Item> itemFactory, Item.Settings settings) {
		// Create the item key.
		RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, name));

		// Create the item instance.
		Item item = itemFactory.apply(settings.registryKey(itemKey));

		// Register the item.
		Registry.register(Registries.ITEM, itemKey, item);

		return item;
	}
	
	public static final ConsumableComponent POISON_FOOD_CONSUMABLE_COMPONENT = ConsumableComponents.food()
			// The duration is in ticks, 20 ticks = 1 second
			.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.POISON, 6 * 20, 2), 1.0f))
			.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 6 * 20, 0), 1.0f))
			.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.HUNGER, 30 * 20, 1), 1.0f))
			.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 60 * 20, 1), 1.0f))
			.consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 5 * 20, 1), 1.0f))
			.build();
	public static final FoodComponent POISON_FOOD_COMPONENT = new FoodComponent.Builder()
			.alwaysEdible()
			.build();
	
	public static final Item Orio = register("orio", Item::new, new Item.Settings().maxCount(16).food(new FoodComponent.Builder().nutrition(6).saturationModifier(0.5f).build()));
	public static final Item Soap = register("soap", SoapItem::new, new Item.Settings().maxCount(16).food(POISON_FOOD_COMPONENT, POISON_FOOD_CONSUMABLE_COMPONENT));
	public static final Item BittenSoap = register("bitten_soap", SoapItem::new, new Item.Settings().maxCount(16).food(POISON_FOOD_COMPONENT, POISON_FOOD_CONSUMABLE_COMPONENT));
	
	public static final Item PLOHT_HELMET = register(
			"ploht_helmet",
			(p) -> new Item(p.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.HELMET)),
			new Item.Settings().maxDamage(EquipmentType.HELMET.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
	);
	public static final Item PLOHT_CHESTPLATE = register(
			"ploht_chestplate",
			(p) -> new Item(p.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.CHESTPLATE)),
			new Item.Settings().maxDamage(EquipmentType.CHESTPLATE.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
	);

	public static final Item PLOHT_LEGGINGS = register(
			"ploht_leggings",
			(p) -> new Item(p.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.LEGGINGS)),
			new Item.Settings().maxDamage(EquipmentType.LEGGINGS.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
	);

	public static final Item PLOHT_BOOTS = register(
			"ploht_boots",
			(p) -> new Item(p.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.BOOTS)),
			new Item.Settings().maxDamage(EquipmentType.BOOTS.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
	);
}