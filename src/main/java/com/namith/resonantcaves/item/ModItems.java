package com.namith.resonantcaves.item;

import com.namith.resonantcaves.ResonantCaves;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.List;

/**
 * Feature 7 — Resonant Helmet: a custom armor material matching vanilla GOLD's
 * defense/toughness/knockback-resistance/enchantability/equip-sound, but with IRON's
 * durability multiplier (165 = base 11 * 15).
 */
public final class ModItems {
	private ModItems() {
	}

	public static final RegistryEntry<ArmorMaterial> RESONANT = Registry.registerReference(
			Registries.ARMOR_MATERIAL,
			Identifier.of(ResonantCaves.MOD_ID, "resonant"),
			new ArmorMaterial(
					buildDefenseMap(),
					25, // enchantability — matches GOLD
					SoundEvents.ITEM_ARMOR_EQUIP_GOLD,
					() -> Ingredient.ofItems(Items.GOLD_INGOT),
					List.of(new ArmorMaterial.Layer(Identifier.of(ResonantCaves.MOD_ID, "resonant_helmet"))),
					0.0F, // toughness — matches GOLD
					0.0F  // knockback resistance — matches GOLD
			));

	public static final Item RESONANT_HELMET = Registry.register(Registries.ITEM,
			Identifier.of(ResonantCaves.MOD_ID, "resonant_helmet"),
			new ArmorItem(RESONANT, ArmorItem.Type.HELMET,
					new Item.Settings().maxDamage(ArmorItem.Type.HELMET.getMaxDamage(15))));

	// Only HELMET is used, but ArmorMaterial expects an entry per type; fill the rest with GOLD's values.
	private static EnumMap<ArmorItem.Type, Integer> buildDefenseMap() {
		EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
		map.put(ArmorItem.Type.HELMET, 2);
		map.put(ArmorItem.Type.CHESTPLATE, 5);
		map.put(ArmorItem.Type.LEGGINGS, 3);
		map.put(ArmorItem.Type.BOOTS, 1);
		map.put(ArmorItem.Type.BODY, 7);
		return map;
	}

	public static void register() {
		ItemGroupEvents.modifyEntriesEvent(RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of("minecraft", "combat")))
				.register(entries -> entries.add(RESONANT_HELMET));
	}
}
