package com.fjmode.client.render;

import net.minecraft.client.renderer.item.ItemStackRenderState;

public interface SwordFlightRenderStateAccess {
	boolean fjmode$isSwordFlightActive();

	void fjmode$setSwordFlightActive(boolean active);

	ItemStackRenderState fjmode$getSwordFlightSwordState();
}
