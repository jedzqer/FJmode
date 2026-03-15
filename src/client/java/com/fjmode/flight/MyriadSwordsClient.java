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
import org.joml.Vector3f;

public final class MyriadSwordsClient {
	private static final float MODEL_FORWARD_X = 0.0F;
	private static final float MODEL_FORWARD_Y = 1.0F;
	private static final float MODEL_FORWARD_Z = 0.0F;
	private static final Map<SwordKey, ClientSwordState> ACTIVE_SWORDS = new HashMap<>();
	private static final Set<UUID> OWNERS_WITH_ACTIVE_SWORDS = new HashSet<>();
	private static final double POSITION_SMOOTHING = 0.33D;
	private static final double VELOCITY_SMOOTHING = 0.24D;
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
			current.position = new Vec3(snapshot.x(), snapshot.y(), snapshot.z());
			current.velocity = new Vec3(snapshot.velocityX(), snapshot.velocityY(), snapshot.velocityZ());
			current.renderState.clear();
			Minecraft client = Minecraft.getInstance();
			client.getItemModelResolver().updateForLiving(
				current.renderState,
				snapshot.stack(),
				ItemDisplayContext.FIXED,
				client.player
			);
			if (!current.initialized) {
				current.renderPosition = current.position;
				if (current.velocity.lengthSqr() > 1.0E-4D) {
					current.renderVelocity = current.velocity.normalize();
				}
				current.initialized = true;
			}
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
			state.renderPosition = state.renderPosition.lerp(state.position, POSITION_SMOOTHING);
			Vec3 targetRenderVelocity = state.velocity.lengthSqr() > 1.0E-4D ? state.velocity.normalize() : state.renderVelocity;
			if (targetRenderVelocity.lengthSqr() <= 1.0E-4D) {
				targetRenderVelocity = new Vec3(0.0D, 0.0D, 1.0D);
			}
			state.renderVelocity = state.renderVelocity.lerp(targetRenderVelocity, VELOCITY_SMOOTHING).normalize();

			Vector3f direction = new Vector3f(
				(float) state.renderVelocity.x,
				(float) state.renderVelocity.y,
				(float) state.renderVelocity.z
			).normalize();
			Quaternionf facingRotation = new Quaternionf().rotationTo(
				MODEL_FORWARD_X,
				MODEL_FORWARD_Y,
				MODEL_FORWARD_Z,
				direction.x,
				direction.y,
				direction.z
			);
			float roll = (float) Math.toRadians(45.0F + 20.0F * Mth.sin(client.level.getGameTime() * 0.2F));
			int packedLight = LevelRenderer.getLightColor(client.level, BlockPos.containing(state.renderPosition));

			poseStack.pushPose();
			poseStack.translate(state.renderPosition.x - cameraPos.x, state.renderPosition.y - cameraPos.y, state.renderPosition.z - cameraPos.z);
			poseStack.mulPose(facingRotation);
			poseStack.mulPose(new Quaternionf().rotationY(roll));
			poseStack.scale(1.35F, 1.35F, 1.35F);
			state.renderState.submit(poseStack, context.commandQueue(), packedLight, 0, 0);
			poseStack.popPose();
		}
	}

	private static final class ClientSwordState {
		private final ItemStackRenderState renderState = new ItemStackRenderState();
		private Vec3 position = Vec3.ZERO;
		private Vec3 velocity = Vec3.ZERO;
		private Vec3 renderPosition = Vec3.ZERO;
		private Vec3 renderVelocity = new Vec3(0.0D, 0.0D, 1.0D);
		private boolean initialized;
	}

	private record SwordKey(UUID ownerId, int swordId) {
	}
}
