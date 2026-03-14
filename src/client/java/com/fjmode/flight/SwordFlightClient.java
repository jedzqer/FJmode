package com.fjmode.flight;

import com.fjmode.network.SwordFlightBoostPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class SwordFlightClient {
	private SwordFlightClient() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(SwordFlightClient::tickClient);
	}

	private static void tickClient(Minecraft client) {
		LocalPlayer player = client.player;
		if (player == null || client.isPaused()) {
			return;
		}

		if (!SwordFlightController.isSwordFlightActive(player)) {
			return;
		}

		if (client.options.keySprint.consumeClick() && ClientPlayNetworking.canSend(SwordFlightBoostPayload.TYPE)) {
			ClientPlayNetworking.send(SwordFlightBoostPayload.INSTANCE);
		}
	}
}
