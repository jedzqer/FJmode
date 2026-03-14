package com.fjmode.enchantment.effect;

import com.mojang.serialization.MapCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

public record EmptyEnchantmentEntityEffect() implements EnchantmentEntityEffect {
	public static final EmptyEnchantmentEntityEffect INSTANCE = new EmptyEnchantmentEntityEffect();
	public static final MapCodec<EmptyEnchantmentEntityEffect> CODEC = MapCodec.unit(INSTANCE);

	@Override
	public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse itemInUse, Entity entity, Vec3 origin) {
	}

	@Override
	public MapCodec<? extends EnchantmentEntityEffect> codec() {
		return CODEC;
	}
}
