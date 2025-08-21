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
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.ComponentMap;

import java.util.function.Function;

public class ModItems {
	public static Item ORIO;	
    public static Item SOAP;
    public static Item BITTEN_SOAP;
    public static Item FOOD_SATCHEL;
    public static Item TORCH_SATCHEL;
    public static Item PLOHT_HELMET;
    public static Item PLOHT_CHESTPLATE;
    public static Item PLOHT_LEGGINGS;
    public static Item PLOHT_BOOTS;
    public static Item SHAFT_BUILDER;
	public static Item GRAPPLING_HOOK;
    
	public static void initialize() {
	}
	
    public static void register() {
        // Create food components first, without any complex dependencies
        FoodComponent simplePoisonFood = new FoodComponent.Builder()
            .alwaysEdible()
            .build();
            
        ConsumableComponent simplePoisonConsumable = ConsumableComponents.food()
            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.POISON, 6 * 20, 2), 1.0f))
            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 6 * 20, 0), 1.0f))
            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.HUNGER, 30 * 20, 1), 1.0f))
            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 60 * 20, 1), 1.0f))
            .consumeEffect(new ApplyEffectsConsumeEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 5 * 20, 1), 1.0f))
            .build();
        
        if (ChatPlaysMcMod.CONFIG.enableOrio()) {
        	ORIO = registerItem("orio", new Item(new Item.Settings()
            		.maxCount(16)
            		.food(new FoodComponent
            				.Builder()
            				.nutrition(6)
            				.saturationModifier(0.5f)
            				.build())
            		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "orio")))));
        }
        
        if (ChatPlaysMcMod.CONFIG.enableSoap()) {
        	SOAP = registerItem("soap", new SoapItem(new Item.Settings()
            		.maxCount(16)
            		.food(simplePoisonFood, simplePoisonConsumable)
            		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "soap")))));
        	
        	BITTEN_SOAP = registerItem("bitten_soap", new SoapItem(new Item.Settings()
            		.maxCount(16)
            		.food(simplePoisonFood, simplePoisonConsumable)
            		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "bitten_soap")))));
		}
        
        if (ChatPlaysMcMod.CONFIG.enableFoodSatchel()) {
			FOOD_SATCHEL = registerItem("food_satchel", new FoodSatchelItem(new Item.Settings()
					.maxCount(1)
					.component(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(java.util.List.of(ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY)))
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "food_satchel")))));
		}
        
        if (ChatPlaysMcMod.CONFIG.enablePlohtArmor()) {
        	PLOHT_HELMET = registerItem("ploht_helmet",
            		new Item(new Item.Settings()
            				.maxDamage(EquipmentType.HELMET
            						.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
            				.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.HELMET)
    						.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "ploht_helmet")))));
            PLOHT_CHESTPLATE = registerItem("ploht_chestplate",
            		new Item(new Item.Settings()
    						.maxDamage(EquipmentType.CHESTPLATE
    								.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
    						.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.CHESTPLATE)
    						.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "ploht_chestplate")))));
            PLOHT_LEGGINGS = registerItem("ploht_leggings",
    				new Item(new Item.Settings()
    						.maxDamage(EquipmentType.LEGGINGS
    								.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
    						.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.LEGGINGS)
    						.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "ploht_leggings")))));
    		PLOHT_BOOTS = registerItem("ploht_boots",
    				new Item(new Item.Settings()
    						.maxDamage(EquipmentType.BOOTS
    								.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
    						.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.BOOTS)
    						.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "ploht_boots")))));
		}
        
        if (ChatPlaysMcMod.CONFIG.enableTorchSatchel()) {
        	TORCH_SATCHEL = registerItem("torch_satchel", new TorchSatchelItem(new Item.Settings()
    				.maxCount(1)
    				.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "torch_satchel")))));
		}
        
        SHAFT_BUILDER = registerItem("shaft_builder", new ShaftBuilder(new Item.Settings()
				.maxCount(16)
				.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "shaft_builder")))));
        
        GRAPPLING_HOOK = registerItem("grappling_hook", new GrapplingHookItem(new Item.Settings()
        		.maxCount(16)
        		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "grappling_hook")))));
    }

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, name), item);
    }
	
	// Keep these as constants but don't use them in registration to avoid circular dependencies
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
//	
//	public static final Item Orio = register("orio", Item::new, new Item.Settings().maxCount(16).food(new FoodComponent.Builder().nutrition(6).saturationModifier(0.5f).build()));
//	public static final Item Soap = register("soap", SoapItem::new, new Item.Settings().maxCount(16).food(POISON_FOOD_COMPONENT, POISON_FOOD_CONSUMABLE_COMPONENT));
//	public static final Item BittenSoap = register("bitten_soap", SoapItem::new, new Item.Settings().maxCount(16).food(POISON_FOOD_COMPONENT, POISON_FOOD_CONSUMABLE_COMPONENT));
//	
//	public static final Item PLOHT_HELMET = register(
//			"ploht_helmet",
//			(p) -> new Item(p.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.HELMET)),
//			new Item.Settings().maxDamage(EquipmentType.HELMET.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
//	);
//	public static final Item PLOHT_CHESTPLATE = register(
//			"ploht_chestplate",
//			(p) -> new Item(p.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.CHESTPLATE)),
//			new Item.Settings().maxDamage(EquipmentType.CHESTPLATE.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
//	);
//
//	public static final Item PLOHT_LEGGINGS = register(
//			"ploht_leggings",
//			(p) -> new Item(p.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.LEGGINGS)),
//			new Item.Settings().maxDamage(EquipmentType.LEGGINGS.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
//	);
//
//	public static final Item PLOHT_BOOTS = register(
//			"ploht_boots",
//			(p) -> new Item(p.armor(PlohtArmorMaterial.INSTANCE, EquipmentType.BOOTS)),
//			new Item.Settings().maxDamage(EquipmentType.BOOTS.getMaxDamage(PlohtArmorMaterial.BASE_DURABILITY))
//	);
}