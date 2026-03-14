package com.fjmode.flight;

import com.fjmode.enchantment.ModEnchantments;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public final class SwordFlightController {
	private static final Map<UUID, Integer> BOOST_COOLDOWNS = new HashMap<>();
	private static final Map<UUID, Boolean> ACTIVE_FLIGHT = new HashMap<>();
	private static final double BASE_GLIDE_ACCELERATION = 0.045D;
	private static final double BASE_BOOST_STRENGTH = 0.9D;
	private static final int BOOST_COOLDOWN_TICKS = 10;

	private SwordFlightController() {
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				tickPlayer(player);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			UUID playerId = handler.player.getUUID();
			BOOST_COOLDOWNS.remove(playerId);
			ACTIVE_FLIGHT.remove(playerId);
		});
	}

	public static boolean canUseSwordFlight(LivingEntity entity) {
		ItemStack weapon = entity.getMainHandItem();
		if (!weapon.is(ItemTags.SWORDS)) {
			return false;
		}

		return getSwordFlightLevel(entity) > 0
			&& !entity.isInWater()
			&& !entity.isInLava()
			&& !entity.isPassenger()
			&& !entity.isSleeping();
	}

	public static int getSwordFlightLevel(LivingEntity entity) {
		return EnchantmentHelper.getItemEnchantmentLevel(
			ModEnchantments.swordFlight(entity.registryAccess()),
			entity.getMainHandItem()
		);
	}

	public static boolean isSwordFlightActive(LivingEntity entity) {
		if (!canUseSwordFlight(entity)) {
			return false;
		}

		if (entity instanceof ServerPlayer serverPlayer) {
			return ACTIVE_FLIGHT.getOrDefault(serverPlayer.getUUID(), false);
		}

		return !entity.onGround()
			&& !entity.isSwimming()
			&& entity.getDeltaMovement().lengthSqr() > 0.08D;
	}

	public static void triggerBoost(ServerPlayer player) {
		if (!isSwordFlightActive(player)) {
			return;
		}

		UUID playerId = player.getUUID();
		if (BOOST_COOLDOWNS.getOrDefault(playerId, 0) > 0) {
			return;
		}

		if (!player.hasInfiniteMaterials() && player.getFoodData().getFoodLevel() <= 0) {
			return;
		}

		int enchantmentLevel = getSwordFlightLevel(player);
		Vec3 boostedVelocity = player.getDeltaMovement().add(
			player.getLookAngle().normalize().scale(BASE_BOOST_STRENGTH + 0.25D * enchantmentLevel)
		);
		double maxSpeed = 2.6D + enchantmentLevel * 0.2D;

		if (boostedVelocity.lengthSqr() > maxSpeed * maxSpeed) {
			boostedVelocity = boostedVelocity.normalize().scale(maxSpeed);
		}

		player.setDeltaMovement(boostedVelocity);
		player.resetFallDistance();
		player.applyPostImpulseGraceTime(10);
		player.hurtMarked = true;

		if (!player.hasInfiniteMaterials()) {
			player.causeFoodExhaustion(1.5F + 0.5F * enchantmentLevel);
		}

		BOOST_COOLDOWNS.put(playerId, BOOST_COOLDOWN_TICKS);
	}

	private static void tickPlayer(ServerPlayer player) {
		UUID playerId = player.getUUID();
		int cooldown = BOOST_COOLDOWNS.getOrDefault(playerId, 0);
		if (cooldown > 0) {
			BOOST_COOLDOWNS.put(playerId, cooldown - 1);
		}

		if (!canUseSwordFlight(player)) {
			ACTIVE_FLIGHT.remove(playerId);
			return;
		}

		if (shouldStartSwordFlight(player)) {
			ACTIVE_FLIGHT.put(playerId, true);
			player.hurtMarked = true;
		}

		if (!ACTIVE_FLIGHT.getOrDefault(playerId, false)) {
			return;
		}

		if (player.onGround() || player.isSwimming() || player.isInWater() || player.isInLava()) {
			ACTIVE_FLIGHT.remove(playerId);
			return;
		}

		Vec3 lookDirection = player.getLookAngle().normalize();
		Vec3 velocity = player.getDeltaMovement();
		Vec3 nextVelocity = velocity.scale(0.985D).add(lookDirection.scale(BASE_GLIDE_ACCELERATION + 0.01D * getSwordFlightLevel(player)));
		double maxHorizontalSpeed = 1.35D;
		double horizontalSpeed = Math.sqrt(nextVelocity.x * nextVelocity.x + nextVelocity.z * nextVelocity.z);

		if (horizontalSpeed > maxHorizontalSpeed) {
			double scale = maxHorizontalSpeed / horizontalSpeed;
			nextVelocity = new Vec3(nextVelocity.x * scale, nextVelocity.y, nextVelocity.z * scale);
		}

		if (nextVelocity.y < -0.9D) {
			nextVelocity = new Vec3(nextVelocity.x, -0.9D, nextVelocity.z);
		}

		player.setDeltaMovement(nextVelocity);
		player.resetFallDistance();
		player.applyPostImpulseGraceTime(5);
		player.hurtMarked = true;

		if (!player.hasInfiniteMaterials() && player.tickCount % 20 == 0) {
			player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		}
	}

	private static boolean shouldStartSwordFlight(ServerPlayer player) {
		return !player.onGround()
			&& !player.onClimbable()
			&& !player.isSwimming()
			&& player.getDeltaMovement().y <= 0.0D;
	}
}
