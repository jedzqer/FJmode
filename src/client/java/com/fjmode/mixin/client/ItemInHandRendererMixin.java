package com.fjmode.mixin.client;

import com.fjmode.flight.MyriadSwordsClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
	@Redirect(
		method = "tick",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;")
	)
	private ItemStack fjmode$hideMainHandSwordWhileMyriadActive(LocalPlayer player) {
		if (MyriadSwordsClient.hasActiveSwords(player.getUUID())) {
			return ItemStack.EMPTY;
		}
		return player.getMainHandItem();
	}
}
