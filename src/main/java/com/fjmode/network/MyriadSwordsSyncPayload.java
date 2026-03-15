package com.fjmode.network;

import com.fjmode.FJModeMod;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public record MyriadSwordsSyncPayload(List<SwordSnapshot> swords) implements CustomPacketPayload {
	public static final Type<MyriadSwordsSyncPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(FJModeMod.MOD_ID, "myriad_swords_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MyriadSwordsSyncPayload> CODEC = StreamCodec.of(
		(buf, payload) -> {
			buf.writeVarInt(payload.swords.size());
			for (SwordSnapshot sword : payload.swords) {
				SwordSnapshot.STREAM_CODEC.encode(buf, sword);
			}
		},
		buf -> {
			int size = buf.readVarInt();
			List<SwordSnapshot> swords = new ArrayList<>(size);
			for (int i = 0; i < size; i++) {
				swords.add(SwordSnapshot.STREAM_CODEC.decode(buf));
			}
			return new MyriadSwordsSyncPayload(swords);
		}
	);

	@Override
	public Type<MyriadSwordsSyncPayload> type() {
		return TYPE;
	}

	public record SwordSnapshot(
		UUID ownerId,
		int id,
		ItemStack stack,
		double x,
		double y,
		double z,
		double velocityX,
		double velocityY,
		double velocityZ
	) {
		public static final StreamCodec<RegistryFriendlyByteBuf, SwordSnapshot> STREAM_CODEC = StreamCodec.of(
			(buf, snapshot) -> {
				buf.writeUUID(snapshot.ownerId);
				buf.writeVarInt(snapshot.id);
				ItemStack.STREAM_CODEC.encode(buf, snapshot.stack);
				buf.writeDouble(snapshot.x);
				buf.writeDouble(snapshot.y);
				buf.writeDouble(snapshot.z);
				buf.writeDouble(snapshot.velocityX);
				buf.writeDouble(snapshot.velocityY);
				buf.writeDouble(snapshot.velocityZ);
			},
			buf -> new SwordSnapshot(
				buf.readUUID(),
				buf.readVarInt(),
				ItemStack.STREAM_CODEC.decode(buf),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble(),
				buf.readDouble()
			)
		);
	}
}
