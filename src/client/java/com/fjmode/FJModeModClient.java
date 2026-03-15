package com.fjmode;

import com.fjmode.flight.MyriadSwordsClient;
import com.fjmode.flight.SwordFlightClient;
import net.fabricmc.api.ClientModInitializer;

public class FJModeModClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MyriadSwordsClient.registerNetworking();
		MyriadSwordsClient.register();
		SwordFlightClient.register();
	}
}
