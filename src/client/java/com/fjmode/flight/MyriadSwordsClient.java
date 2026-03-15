package com.fjmode.flight;

import com.fjmode.network.MyriadSwordsSyncPayload;
import com.fjmode.network.MyriadSwordsSyncPayload.SwordSnapshot;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public final class MyriadSwordsClient {
	private static final Map<SwordKey, ClientSwordState> ACTIVE_SWORDS = new HashMap<>();
	private static final Set<UUID> OWNERS_WITH_ACTIVE_SWORDS = new HashSet<>();
	private static boolean networkingRegistered;

	private MyriadSwordsClient() {
	}

	public static void register() {
		WorldRenderEvents.AFTER_ENTITIES.register(MyriadSwordsClient::renderSwords);
	}

	public static void registerNetworking() {
		if (networkingRegistered) {
			return;
		}
		networkingRegistered = true;
		ClientPlayNetworking.registerGlobalReceiver(MyriadSwordsSyncPayload.TYPE, (payload, context) -> applySnapshot(payload.swords()));
	}

	private static void applySnapshot(List<SwordSnapshot> swords) {
		Map<SwordKey, ClientSwordState> next = new HashMap<>(swords.size());
		Set<UUID> nextOwners = new HashSet<>();
		for (SwordSnapshot snapshot : swords) {
			SwordKey key = new SwordKey(snapshot.ownerId(), snapshot.id());
			ClientSwordState previous = ACTIVE_SWORDS.get(key);
			ClientSwordState current = previous == null ? new ClientSwordState() : previous;
			current.previousPosition = current.position;
			current.position = new Vec3(snapshot.x(), snapshot.y(), snapshot.z());
			current.velocity = new Vec3(snapshot.velocityX(), snapshot.velocityY(), snapshot.velocityZ());
			current.renderState.clear();
			Minecraft client = Minecraft.getInstance();
			current.renderState.clear();
			client.getItemModelResolver().updateForLiving(
				current.renderState,
				snapshot.stack(),
				ItemDisplayContext.FIXED,
				client.player
			);
			next.put(key, current);
			nextOwners.add(snapshot.ownerId());
		}
		ACTIVE_SWORDS.clear();
		ACTIVE_SWORDS.putAll(next);
		OWNERS_WITH_ACTIVE_SWORDS.clear();
		OWNERS_WITH_ACTIVE_SWORDS.addAll(nextOwners);
	}

	public static boolean hasActiveSwords(UUID ownerId) {
		return OWNERS_WITH_ACTIVE_SWORDS.contains(ownerId);
	}

	private static void renderSwords(WorldRenderContext context) {
		Minecraft client = Minecraft.getInstance();
		if (client.level == null || client.player == null || ACTIVE_SWORDS.isEmpty()) {
			return;
		}

		Vec3 cameraPos = context.worldState().cameraRenderState.pos;
		PoseStack poseStack = context.matrices();
		for (ClientSwordState state : ACTIVE_SWORDS.values()) {
			Vec3 interpolated = state.previousPosition.lerp(state.position, client.getDeltaTracker().getGameTimeDeltaPartialTick(false));
			Vec3 velocity = state.velocity.lengthSqr() > 1.0E-4D ? state.velocity.normalize() : new Vec3(0.0D, 0.0D, 1.0D);
			float yaw = (float) Math.toDegrees(Math.atan2(velocity.x, velocity.z));
			float pitch = (float) -Math.toDegrees(Math.atan2(velocity.y, Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)));
			int packedLight = LevelRenderer.getLightColor(client.level, BlockPos.containing(interpolated));

			poseStack.pushPose();
			poseStack.translate(interpolated.x - cameraPos.x, interpolated.y - cameraPos.y, interpolated.z - cameraPos.z);
			poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180.0F - yaw)));
			poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(pitch + 90.0F)));
			poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(20.0F * Mth.sin(client.level.getGameTime() * 0.2F))));
			poseStack.scale(1.35F, 1.35F, 1.35F);
			state.renderState.submit(poseStack, context.commandQueue(), packedLight, 0, 0);
			poseStack.popPose();
		}
	}

	private static final class ClientSwordState {
		private final ItemStackRenderState renderState = new ItemStackRenderState();
		private Vec3 previousPosition = Vec3.ZERO;
		private Vec3 position = Vec3.ZERO;
		private Vec3 velocity = Vec3.ZERO;
	}

	private record SwordKey(UUID ownerId, int swordId) {
	}
}
