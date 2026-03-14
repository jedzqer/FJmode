package com.fjmode.network;

import com.fjmode.FJModeMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SwordFlightBoostPayload() implements CustomPacketPayload {
	public static final SwordFlightBoostPayload INSTANCE = new SwordFlightBoostPayload();
	public static final Type<SwordFlightBoostPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FJModeMod.MOD_ID, "sword_flight_boost"));
	public static final StreamCodec<RegistryFriendlyByteBuf, SwordFlightBoostPayload> CODEC = StreamCodec.unit(INSTANCE);

	@Override
	public Type<SwordFlightBoostPayload> type() {
		return TYPE;
	}
}
