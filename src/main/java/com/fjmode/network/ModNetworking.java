package com.fjmode.network;

import com.fjmode.flight.SwordFlightController;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class ModNetworking {
	private ModNetworking() {
	}

	public static void register() {
		PayloadTypeRegistry.playC2S().register(SwordFlightBoostPayload.TYPE, SwordFlightBoostPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MyriadSwordsSyncPayload.TYPE, MyriadSwordsSyncPayload.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(SwordFlightBoostPayload.TYPE, (payload, context) ->
			SwordFlightController.triggerBoost(context.player())
		);
	}
}
