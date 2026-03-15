package com.fjmode;

import com.fjmode.client.render.GroundedSwordRenderer;
import com.fjmode.entity.ModEntities;
import com.fjmode.flight.MyriadSwordsClient;
import com.fjmode.flight.SwordFlightClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public class FJModeModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.GROUNDED_SWORD, GroundedSwordRenderer::new);
		MyriadSwordsClient.registerNetworking();
		MyriadSwordsClient.register();
		SwordFlightClient.register();
	}
}
