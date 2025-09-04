package com.voltaicgrid.chatplaysmc;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.List;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BlocksAttacksComponent;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.ConsumableComponents;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.component.type.ToolComponent;
import net.minecraft.component.type.EnchantableComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.block.Block;
import net.minecraft.item.ToolMaterial;
import net.minecraft.item.consume.ApplyEffectsConsumeEffect;
import net.minecraft.item.equipment.EquipmentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.util.Identifier;
import net.minecraft.sound.SoundEvents;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.Registries;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.component.type.ItemEnchantmentsComponent;

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
    public static Item UPPIES;
    public static Item GRAVE_PEARL;
    public static Item ARITSUKI_SHIELD;
    public static Item MAGNET;
    
    public static Item WOOD_PAXEL;
    public static Item STONE_PAXEL;
    public static Item IRON_PAXEL;
    public static Item GOLD_PAXEL;
    public static Item DIAMOND_PAXEL;
    public static Item NETHERITE_PAXEL;

    private static final TagKey<Block> MINEABLE_AXE =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "mineable/axe"));
    private static final TagKey<Block> MINEABLE_PICKAXE =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "mineable/pickaxe"));
    private static final TagKey<Block> MINEABLE_SHOVEL =
            TagKey.of(RegistryKeys.BLOCK, Identifier.of("minecraft", "mineable/shovel"));

    
    private static RegistryEntryList<Block> tagList(TagKey<Block> tagKey) {
        // Overload present in 1.21.8: owner + tag
        return RegistryEntryList.of(Registries.BLOCK, tagKey);
    }

	public static final BlocksAttacksComponent SHIELD_COMPONENT =
			new BlocksAttacksComponent(
				0.25F,
				1.0F,
				List.of(new BlocksAttacksComponent.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
				new BlocksAttacksComponent.ItemDamage(3.0F, 1.0F, 1.0F),
				Optional.of(DamageTypeTags.BYPASSES_SHIELD),
				Optional.of(SoundEvents.ITEM_SHIELD_BLOCK),
				Optional.of(SoundEvents.ITEM_SHIELD_BREAK)
			);

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
                    .food(new FoodComponent.Builder()
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

		if (ChatPlaysMcMod.CONFIG.enableShaftBuilder()) {
			SHAFT_BUILDER = registerItem("shaft_builder", new ShaftBuilder(new Item.Settings()
					.maxCount(16)
					.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "shaft_builder")))));
		}

        if (ChatPlaysMcMod.CONFIG.enableUppies()) {
            UPPIES = registerItem("uppies", new UppiesItem(new Item.Settings()
                    .maxCount(16)
                    .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "uppies")))));
        }

        GRAVE_PEARL = registerItem("grave_pearl", new GravePearlItem(new Item.Settings()
                .maxCount(16)
                .registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "grave_pearl")))));
        
        ARITSUKI_SHIELD = registerItem("aritsuki_shield", new AritsukiShieldItem(new Item.Settings()
        		.maxCount(1)
        		.component(DataComponentTypes.BLOCKS_ATTACKS, SHIELD_COMPONENT)
				.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "aritsuki_shield")))));
        
        MAGNET = registerItem("magnet", new MagnetItem(new Item.Settings()
				.maxCount(1)
				.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "magnet")))));
        
//        WOOD_PAXEL = registerItem("wooden_paxel", new PaxelItem(new Item.Settings()
//        		.maxCount(1)
//        		.maxDamage(59)
//        		.component(
//                    DataComponentTypes.TOOL,
//                    new ToolComponent(
//                        List.of(
//                            // Rule: (blocks, optSpeed, optCorrectForDrops, optMiningLevelOverride)
//                            new ToolComponent.Rule(tagList(MINEABLE_AXE),     Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_PICKAXE), Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_SHOVEL),  Optional.of(2.0F), Optional.of(true))
//                        ),
//                        1.0F,   // default mining speed when no rule matches
//                        0,      // mining level
//                        true    // damage per block broken (typical for tools)
//                    )
//                )
//                .component(
//                    DataComponentTypes.ENCHANTABLE, new EnchantableComponent(15)
//                )
//        		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "wooden_paxel"))), 1, 1, 0, 15));
//
//        STONE_PAXEL = registerItem("stone_paxel", new PaxelItem(new Item.Settings()
//        		.maxCount(1)
//        		.maxDamage(131)
//        		.component(
//                    DataComponentTypes.TOOL,
//                    new ToolComponent(
//                        List.of(
//                            // Rule: (blocks, optSpeed, optCorrectForDrops, optMiningLevelOverride)
//                            new ToolComponent.Rule(tagList(MINEABLE_AXE),     Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_PICKAXE), Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_SHOVEL),  Optional.of(2.0F), Optional.of(true))
//                        ),
//                        1.0F,   // default mining speed when no rule matches
//                        1,      // mining level
//                        true    // damage per block broken (typical for tools)
//                    )
//                )
//                .component(
//                    DataComponentTypes.ENCHANTABLE, new EnchantableComponent(5)
//                )
//        		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "stone_paxel"))), 2, 1, 1, 5));
//
//        IRON_PAXEL = registerItem("iron_paxel", new PaxelItem(new Item.Settings()
//        		.maxCount(1)
//        		.maxDamage(250)
//        		.component(
//                    DataComponentTypes.TOOL,
//                    new ToolComponent(
//                        List.of(
//                            // Rule: (blocks, optSpeed, optCorrectForDrops, optMiningLevelOverride)
//                            new ToolComponent.Rule(tagList(MINEABLE_AXE),     Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_PICKAXE), Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_SHOVEL),  Optional.of(2.0F), Optional.of(true))
//                        ),
//                        1.0F,   // default mining speed when no rule matches
//                        2,      // mining level
//                        true    // damage per block broken (typical for tools)
//                    )
//                )
//                .component(
//                    DataComponentTypes.ENCHANTABLE, new EnchantableComponent(15)
//                )
//        		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "iron_paxel"))), 2, 2, 2, 14));
//
//        GOLD_PAXEL = registerItem("gold_paxel", new PaxelItem(new Item.Settings()
//        		.maxCount(1)
//        		.maxDamage(32)
//        		.component(
//                    DataComponentTypes.TOOL,
//                    new ToolComponent(
//                        List.of(
//                            // Rule: (blocks, optSpeed, optCorrectForDrops, optMiningLevelOverride)
//                            new ToolComponent.Rule(tagList(MINEABLE_AXE),     Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_PICKAXE), Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_SHOVEL),  Optional.of(2.0F), Optional.of(true))
//                        ),
//                        1.0F,   // default mining speed when no rule matches
//                        0,      // mining level
//                        true    // damage per block broken (typical for tools)
//                    )
//                )
//                .component(
//                    DataComponentTypes.ENCHANTABLE, new EnchantableComponent(22)
//                )
//        		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "gold_paxel"))), 5, 5, 0, 22));
//
//        DIAMOND_PAXEL = registerItem("diamond_paxel", new PaxelItem(new Item.Settings()
//        		.maxCount(1)
//        		.maxDamage(1561)
//        		.component(
//                    DataComponentTypes.TOOL,
//                    new ToolComponent(
//                        List.of(
//                            // Rule: (blocks, optSpeed, optCorrectForDrops, optMiningLevelOverride)
//                            new ToolComponent.Rule(tagList(MINEABLE_AXE),     Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_PICKAXE), Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_SHOVEL),  Optional.of(2.0F), Optional.of(true))
//                        ),
//                        1.0F,   // default mining speed when no rule matches
//                        2,      // mining level
//                        true    // damage per block broken (typical for tools)
//                    )
//                )
//                .component(
//                    DataComponentTypes.ENCHANTABLE, new EnchantableComponent(10)
//                )
//        		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "diamond_paxel"))), 2, 3, 2, 10));
//
//        NETHERITE_PAXEL = registerItem("netherite_paxel", new PaxelItem(new Item.Settings()
//        		.maxCount(1)
//        		.maxDamage(2031)
//        		.component(
//                    DataComponentTypes.TOOL,
//                    new ToolComponent(
//                        List.of(
//                            // Rule: (blocks, optSpeed, optCorrectForDrops, optMiningLevelOverride)
//                            new ToolComponent.Rule(tagList(MINEABLE_AXE),     Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_PICKAXE), Optional.of(2.0F), Optional.of(true)),
//                            new ToolComponent.Rule(tagList(MINEABLE_SHOVEL),  Optional.of(2.0F), Optional.of(true))
//                        ),
//                        1.0F,   // default mining speed when no rule matches
//                        3,      // mining level
//                        true    // damage per block broken (typical for tools)
//                    )
//                )
//                .component(
//                    DataComponentTypes.ENCHANTABLE, new EnchantableComponent(15)
//                )
//        		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "netherite_paxel"))), 3, 3, 3, 15));

        // GRAPPLING_HOOK = registerItem("grappling_hook", new GrapplingHookItem(new Item.Settings()
        // 		.maxCount(16)
        // 		.registryKey(RegistryKey.of(RegistryKeys.ITEM, Identifier.of(ChatPlaysMcMod.MOD_ID, "grappling_hook")))));
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
