package com.fjmode.enchantment;

import com.fjmode.FJModeMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;

public final class ModEnchantments {
	public static final ResourceKey<Enchantment> SWORD_FLIGHT = ResourceKey.create(
		Registries.ENCHANTMENT,
		Identifier.fromNamespaceAndPath(FJModeMod.MOD_ID, "sword_flight")
	);

	private ModEnchantments() {
	}

	public static Holder.Reference<Enchantment> swordFlight(net.minecraft.core.RegistryAccess registryAccess) {
		return registryAccess.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(SWORD_FLIGHT);
	}
}
