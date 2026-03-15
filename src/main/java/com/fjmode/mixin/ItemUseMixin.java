package com.fjmode.mixin;

import com.fjmode.FJModeMod;
import com.fjmode.flight.MyriadSwordsController;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class ItemUseMixin {
	private static final ResourceKey<Enchantment> MYRIAD_SWORDS_RETURN = ResourceKey.create(
		net.minecraft.core.registries.Registries.ENCHANTMENT,
		net.minecraft.resources.Identifier.fromNamespaceAndPath(FJModeMod.MOD_ID, "myriad_swords_return")
	);

	@Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
	private void fjmode$useSpearAnimation(ItemStack stack, CallbackInfoReturnable<ItemUseAnimation> cir) {
		if (hasMyriadEnchant(stack)) {
			cir.setReturnValue(ItemUseAnimation.SPEAR);
		}
	}

	@Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
	private void fjmode$extendUseDuration(ItemStack stack, LivingEntity livingEntity, CallbackInfoReturnable<Integer> cir) {
		if (livingEntity instanceof Player player && MyriadSwordsController.isMyriadSword(stack, player)) {
			cir.setReturnValue(72000);
		}
	}

	@Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
	private void fjmode$releaseMyriadSword(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft, CallbackInfoReturnable<Boolean> cir) {
		if (MyriadSwordsController.releaseUsing(stack, level, livingEntity, timeLeft)) {
			cir.setReturnValue(true);
		}
	}

	private static boolean hasMyriadEnchant(ItemStack stack) {
		ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			if (enchantment.unwrapKey().isPresent() && enchantment.unwrapKey().get().equals(MYRIAD_SWORDS_RETURN)) {
				return true;
			}
		}
		return false;
	}
}
