package com.fjmode.enchantment.effect;

import com.fjmode.FJModeMod;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;

public final class ModEnchantmentEffects {
	public static final MapCodec<EmptyEnchantmentEntityEffect> EMPTY_ENTITY_EFFECT = register(
		"empty_entity_effect",
		EmptyEnchantmentEntityEffect.CODEC
	);

	private ModEnchantmentEffects() {
	}

	public static void register() {
		FJModeMod.LOGGER.info("Registered custom enchantment effect types for {}", FJModeMod.MOD_ID);
	}

	private static <T extends EnchantmentEntityEffect> MapCodec<T> register(String path, MapCodec<T> codec) {
		return Registry.register(
			BuiltInRegistries.ENCHANTMENT_ENTITY_EFFECT_TYPE,
			Identifier.fromNamespaceAndPath(FJModeMod.MOD_ID, path),
			codec
		);
	}
}
