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
	private static final Map<UUID, Integer> ACTIVE_BOOST_TICKS = new HashMap<>();
	private static final Map<UUID, Integer> BOOST_COOLDOWN_TICKS = new HashMap<>();
	private static final Map<UUID, Boolean> ACTIVE_FLIGHT = new HashMap<>();
	private static final int BOOST_FLIGHT_DURATION = 1;
	private static final int BOOST_COOLDOWN_DURATION = 6;
	private static final int DURABILITY_LOSS_INTERVAL_TICKS = 40;
	private static final float BOOST_EXHAUSTION_BASE = 1.75F;
	private static final float BOOST_EXHAUSTION_PER_LEVEL = 0.6F;
	private static final double BASE_GRAVITY = 0.08D;

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
			ACTIVE_BOOST_TICKS.remove(playerId);
			BOOST_COOLDOWN_TICKS.remove(playerId);
			ACTIVE_FLIGHT.remove(playerId);
		});
	}

	public static boolean canUseSwordFlight(LivingEntity entity) {
		if (!hasSwordFlightWeapon(entity)) {
			return false;
		}

		return !entity.isInWater()
			&& !entity.isInLava()
			&& !entity.isPassenger()
			&& !entity.isSleeping();
	}

	public static boolean hasSwordFlightWeapon(LivingEntity entity) {
		ItemStack weapon = entity.getMainHandItem();
		return weapon.is(ItemTags.SWORDS) && getSwordFlightLevel(entity) > 0;
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
		if (BOOST_COOLDOWN_TICKS.getOrDefault(playerId, 0) > 0) {
			return;
		}

		if (!player.hasInfiniteMaterials() && player.getFoodData().getFoodLevel() <= 0) {
			return;
		}

		int enchantmentLevel = getSwordFlightLevel(player);
		player.resetFallDistance();
		player.applyPostImpulseGraceTime(10);
		player.hurtMarked = true;

		if (!player.hasInfiniteMaterials()) {
			player.causeFoodExhaustion(BOOST_EXHAUSTION_BASE + BOOST_EXHAUSTION_PER_LEVEL * enchantmentLevel);
		}

		int boostDuration = getVanillaLikeBoostDuration(player);
		int remainingBoostTicks = ACTIVE_BOOST_TICKS.getOrDefault(playerId, 0);
		ACTIVE_BOOST_TICKS.put(playerId, remainingBoostTicks + boostDuration);
		BOOST_COOLDOWN_TICKS.put(playerId, BOOST_COOLDOWN_DURATION);
	}

	private static void tickPlayer(ServerPlayer player) {
		UUID playerId = player.getUUID();
		int boostTicks = ACTIVE_BOOST_TICKS.getOrDefault(playerId, 0);
		if (boostTicks > 0) {
			ACTIVE_BOOST_TICKS.put(playerId, boostTicks - 1);
		}

		int boostCooldownTicks = BOOST_COOLDOWN_TICKS.getOrDefault(playerId, 0);
		if (boostCooldownTicks > 0) {
			BOOST_COOLDOWN_TICKS.put(playerId, boostCooldownTicks - 1);
		}

		if (!canUseSwordFlight(player)) {
			ACTIVE_FLIGHT.remove(playerId);
			ACTIVE_BOOST_TICKS.remove(playerId);
			BOOST_COOLDOWN_TICKS.remove(playerId);
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
			ACTIVE_BOOST_TICKS.remove(playerId);
			BOOST_COOLDOWN_TICKS.remove(playerId);
			return;
		}

		Vec3 nextVelocity = applyElytraLikeFlight(player);
		if (boostTicks > 0) {
			nextVelocity = applyFireworkLikeBoost(player, nextVelocity);
		}

		player.setDeltaMovement(nextVelocity);
		player.resetFallDistance();
		player.applyPostImpulseGraceTime(5);
		player.hurtMarked = true;

		if (!player.hasInfiniteMaterials() && player.tickCount % DURABILITY_LOSS_INTERVAL_TICKS == 0) {
			player.getMainHandItem().hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
		}
	}

	private static boolean shouldStartSwordFlight(ServerPlayer player) {
		return !player.onGround()
			&& !player.onClimbable()
			&& !player.isSwimming()
			&& player.getDeltaMovement().y <= 0.0D;
	}

	private static Vec3 applyElytraLikeFlight(ServerPlayer player) {
		Vec3 lookDirection = player.getLookAngle();
		Vec3 velocity = player.getDeltaMovement();
		double horizontalLookLength = Math.sqrt(lookDirection.x * lookDirection.x + lookDirection.z * lookDirection.z);
		double horizontalSpeed = Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
		double speed = velocity.length();
		double pitchRadians = Math.toRadians(player.getXRot());
		double pitchCos = Math.cos(pitchRadians);
		double pitchCosSquared = pitchCos * pitchCos;

		Vec3 nextVelocity = velocity.add(0.0D, BASE_GRAVITY * (-1.0D + pitchCosSquared * 0.75D), 0.0D);

		if (nextVelocity.y < 0.0D && horizontalLookLength > 0.0D) {
			double lift = nextVelocity.y * -0.1D * pitchCosSquared;
			nextVelocity = nextVelocity.add(
				lookDirection.x * lift / horizontalLookLength,
				lift,
				lookDirection.z * lift / horizontalLookLength
			);
		}

		if (pitchRadians < 0.0D && horizontalLookLength > 0.0D) {
			double divePull = horizontalSpeed * -Math.sin(pitchRadians) * 0.04D;
			nextVelocity = nextVelocity.add(
				-lookDirection.x * divePull / horizontalLookLength,
				divePull * 3.2D,
				-lookDirection.z * divePull / horizontalLookLength
			);
		}

		if (horizontalLookLength > 0.0D) {
			nextVelocity = nextVelocity.add(
				(lookDirection.x / horizontalLookLength * speed - nextVelocity.x) * 0.1D,
				0.0D,
				(lookDirection.z / horizontalLookLength * speed - nextVelocity.z) * 0.1D
			);
		}

		return nextVelocity.multiply(0.99D, 0.98D, 0.99D);
	}

	private static Vec3 applyFireworkLikeBoost(ServerPlayer player, Vec3 velocity) {
		Vec3 lookDirection = player.getLookAngle();
		return velocity.add(
			lookDirection.x * 0.1D + (lookDirection.x * 1.5D - velocity.x) * 0.5D,
			lookDirection.y * 0.1D + (lookDirection.y * 1.5D - velocity.y) * 0.5D,
			lookDirection.z * 0.1D + (lookDirection.z * 1.5D - velocity.z) * 0.5D
		);
	}

	private static int getVanillaLikeBoostDuration(ServerPlayer player) {
		return 10 * (BOOST_FLIGHT_DURATION + 1) + player.getRandom().nextInt(6) + player.getRandom().nextInt(7);
	}
}
