package com.fjmode;

import com.fjmode.enchantment.effect.ModEnchantmentEffects;
import com.fjmode.entity.ModEntities;
import com.fjmode.flight.MyriadSwordsController;
import com.fjmode.flight.SwordFlightController;
import com.fjmode.network.ModNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FJModeMod implements ModInitializer {
	public static final String MOD_ID = "fjmode";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEnchantmentEffects.register();
		ModEntities.register();
		ModNetworking.register();
		MyriadSwordsController.register();
		SwordFlightController.register();
		LOGGER.info("Initialized {}", MOD_ID);
	}
}
