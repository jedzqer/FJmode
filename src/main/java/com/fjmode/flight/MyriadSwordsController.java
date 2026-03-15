package com.fjmode.flight;

import com.fjmode.enchantment.ModEnchantments;
import com.fjmode.network.MyriadSwordsSyncPayload;
import com.fjmode.network.MyriadSwordsSyncPayload.SwordSnapshot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;

public final class MyriadSwordsController {
	private static final Map<UUID, SwordPool> POOLS = new HashMap<>();
	private static final int MIN_CHARGE_TICKS = 10;
	private static final int BASE_POOL_SIZE = 8;
	private static final int EXTRA_POOL_SIZE_PER_LEVEL = 4;
	private static final int SYNC_INTERVAL = 1;
	private static final int MAX_NEIGHBORS = 6;
	private static final int LIFETIME_TICKS = 20 * 60;
	private static final int STRIKE_COOLDOWN_TICKS = 10;
	private static final int QUICK_GATHER_TICKS = 12;
	private static final double LAUNCH_SPEED = 0.7D;
	private static final double MAX_SPEED = 1.15D;
	private static final double BOID_NEIGHBOR_RANGE = 4.8D;
	private static final double BOID_SEPARATION_RANGE = 0.85D;
	private static final double SEPARATION_WEIGHT = 0.08D;
	private static final double ALIGNMENT_WEIGHT = 0.04D;
	private static final double COHESION_WEIGHT = 0.045D;
	private static final double ORBIT_PULL_WEIGHT = 0.18D;
	private static final double QUICK_GATHER_WEIGHT = 0.42D;
	private static final double ORBIT_TANGENT_WEIGHT = 0.12D;
	private static final double TARGET_WEIGHT = 0.28D;
	private static final double DRAG = 0.92D;
	private static final double TARGET_HIT_RANGE = 1.6D;
	private static final double ORBIT_RADIUS = 2.75D;
	private static final double ORBIT_RADIUS_STEP = 0.45D;
	private static final double ORBIT_HEIGHT = 1.15D;
	private static final double ORBIT_VERTICAL_WAVE = 0.32D;

	private MyriadSwordsController() {
	}

	public static void register() {
		AttackEntityCallback.EVENT.register(MyriadSwordsController::markAttackTarget);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerLevel level : server.getAllLevels()) {
				tickLevel(level);
			}
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> POOLS.remove(handler.player.getUUID()));
	}

	public static boolean isMyriadSword(ItemStack stack, Player player) {
		return stack.is(ItemTags.SWORDS)
			&& EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.myriadSwordsReturn(player.registryAccess()), stack) > 0;
	}

	public static boolean releaseUsing(ItemStack stack, net.minecraft.world.level.Level level, LivingEntity user, int timeLeft) {
		if (!(user instanceof ServerPlayer player) || !isMyriadSword(stack, player)) {
			return false;
		}

		int useTicks = stack.getUseDuration(player) - timeLeft;
		if (useTicks < MIN_CHARGE_TICKS) {
			return false;
		}

		ItemStack launchedStack = stack.copyWithCount(1);
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
		launchSword(player, launchedStack);
		level.playSound(null, player, SoundEvents.TRIDENT_THROW.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
		return true;
	}

	public static boolean hasActiveSwords(Player player) {
		SwordPool pool = POOLS.get(player.getUUID());
		return pool != null && pool.hasActiveSwords();
	}

	public static boolean tryAssignTarget(Player player, Entity entity) {
		if (!(player instanceof ServerPlayer serverPlayer) || !isMyriadSword(player.getMainHandItem(), player)) {
			return false;
		}

		SwordPool pool = POOLS.get(serverPlayer.getUUID());
		if (pool == null || !pool.hasActiveSwords()) {
			return false;
		}

		pool.setTarget(entity);
		return true;
	}

	private static InteractionResult markAttackTarget(Player player, net.minecraft.world.level.Level level, InteractionHand hand, Entity entity, net.minecraft.world.phys.EntityHitResult hitResult) {
		if (!(player instanceof ServerPlayer serverPlayer) || !isMyriadSword(player.getItemInHand(hand), player)) {
			return InteractionResult.PASS;
		}

		SwordPool pool = POOLS.get(serverPlayer.getUUID());
		if (pool != null && pool.hasActiveSwords()) {
			pool.setTarget(entity);
		}
		return InteractionResult.PASS;
	}

	private static void launchSword(ServerPlayer player, ItemStack visualStack) {
		SwordPool pool = POOLS.computeIfAbsent(player.getUUID(), ignored -> new SwordPool());
		int level = ModEnchantments.getMyriadSwordsReturnLevel(player);
		int maxCapacity = BASE_POOL_SIZE + level * EXTRA_POOL_SIZE_PER_LEVEL;
		VirtualSword sword = pool.acquire(maxCapacity);
		if (sword == null) {
			return;
		}

		Vec3 look = player.getLookAngle().normalize();
		Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
		if (right.lengthSqr() < 1.0E-4D) {
			right = new Vec3(1.0D, 0.0D, 0.0D);
		}
		right = right.normalize();
		Vec3 up = right.cross(look).normalize();
		int ordinal = pool.activeCount % 5;
		double sideOffset = (ordinal - 2) * 0.28D;
		Vec3 spawnPos = player.getEyePosition()
			.add(look.scale(0.9D))
			.add(right.scale(sideOffset))
			.add(up.scale(-0.18D + (ordinal % 2) * 0.08D));

		sword.activate(
			(ServerLevel) player.level(),
			player.getUUID(),
			visualStack,
			spawnPos,
			look.scale(LAUNCH_SPEED).add(player.getDeltaMovement().scale(0.35D)),
			look
		);
		player.swing(InteractionHand.MAIN_HAND, true);
	}

	private static void tickLevel(ServerLevel level) {
		List<VirtualSword> active = new ArrayList<>();
		for (SwordPool pool : POOLS.values()) {
			pool.collectActive(level, active);
			pool.tickTarget(level);
			pool.tryStrikeTarget(level);
		}

		if (!active.isEmpty()) {
			List<Vec3> nextVelocities = computeBoidVelocities(active);
			for (int i = 0; i < active.size(); i++) {
				active.get(i).tick(nextVelocities.get(i));
			}
		}

		if (level.getGameTime() % SYNC_INTERVAL == 0L) {
			syncLevel(level, active);
		}
	}

	private static List<Vec3> computeBoidVelocities(List<VirtualSword> active) {
		List<Vec3> velocities = new ArrayList<>(active.size());
		for (int i = 0; i < active.size(); i++) {
			VirtualSword sword = active.get(i);
			Entity target = sword.pool.getTargetEntity(sword.level);
			boolean trackingTarget = target != null;
			ServerPlayer owner = sword.owner(sword.level);
			Vec3 separation = Vec3.ZERO;
			Vec3 alignment = Vec3.ZERO;
			Vec3 cohesion = Vec3.ZERO;
			int neighbors = 0;

			for (int j = 0; j < active.size(); j++) {
				if (i == j) {
					continue;
				}

				VirtualSword other = active.get(j);
				Vec3 offset = sword.position.subtract(other.position);
				double distance = offset.length();
				if (distance <= 1.0E-4D || distance > BOID_NEIGHBOR_RANGE) {
					continue;
				}

				if (!trackingTarget && distance < BOID_SEPARATION_RANGE) {
					separation = separation.add(offset.normalize().scale((BOID_SEPARATION_RANGE - distance) / BOID_SEPARATION_RANGE));
				}

				if (neighbors < MAX_NEIGHBORS) {
					alignment = alignment.add(other.velocity);
					cohesion = cohesion.add(other.position);
					neighbors++;
				}
			}

			Vec3 steering = sword.velocity.scale(DRAG);
			if (neighbors > 0) {
				Vec3 averageVelocity = alignment.scale(1.0D / neighbors);
				if (averageVelocity.lengthSqr() > 1.0E-4D) {
					averageVelocity = averageVelocity.normalize().scale(MAX_SPEED);
				}
				Vec3 center = cohesion.scale(1.0D / neighbors);
				steering = steering
					.add(separation.scale(trackingTarget ? 0.0D : SEPARATION_WEIGHT))
					.add(averageVelocity.subtract(sword.velocity).scale(ALIGNMENT_WEIGHT))
					.add(center.subtract(sword.position).scale(COHESION_WEIGHT));
			}

			Vec3 orbitSteering = Vec3.ZERO;
			if (!trackingTarget && owner != null) {
				Vec3 orbitAnchor = sword.computeOrbitAnchor(owner);
				Vec3 toAnchor = orbitAnchor.subtract(sword.position);
				if (toAnchor.lengthSqr() > 1.0E-4D) {
					double gatherWeight = sword.ageTicks < QUICK_GATHER_TICKS ? QUICK_GATHER_WEIGHT : ORBIT_PULL_WEIGHT;
					orbitSteering = orbitSteering.add(
						toAnchor.normalize().scale(MAX_SPEED).subtract(sword.velocity).scale(gatherWeight)
					);
				}

				Vec3 radial = sword.position.subtract(sword.computeOrbitCenter(owner));
				if (radial.lengthSqr() > 1.0E-4D) {
					Vec3 tangent = new Vec3(-radial.z, 0.0D, radial.x).normalize().scale(MAX_SPEED * 0.7D);
					orbitSteering = orbitSteering.add(tangent.subtract(sword.velocity).scale(ORBIT_TANGENT_WEIGHT));
				}
			}

			Vec3 targetSteering = orbitSteering;
			if (trackingTarget) {
				Vec3 targetPoint = target.getBoundingBox().getCenter();
				Vec3 toTarget = targetPoint.subtract(sword.position);
				if (toTarget.lengthSqr() > 1.0E-4D) {
					targetSteering = targetSteering.add(
						toTarget.normalize().scale(MAX_SPEED).subtract(sword.velocity).scale(TARGET_WEIGHT)
					);
				}
			}

			Vec3 nextVelocity = steering.add(targetSteering);
			if (nextVelocity.lengthSqr() < 1.0E-4D) {
				nextVelocity = sword.forward.scale(LAUNCH_SPEED);
			}
			if (nextVelocity.length() > MAX_SPEED) {
				nextVelocity = nextVelocity.normalize().scale(MAX_SPEED);
			}
			velocities.add(nextVelocity);
		}
		return velocities;
	}

	private static void syncLevel(ServerLevel level, List<VirtualSword> active) {
		List<SwordSnapshot> snapshots = new ArrayList<>(active.size());
		for (VirtualSword sword : active) {
			snapshots.add(new SwordSnapshot(
				sword.ownerId,
				sword.id,
				sword.visualStack,
				sword.position.x,
				sword.position.y,
				sword.position.z,
				sword.velocity.x,
				sword.velocity.y,
				sword.velocity.z
			));
		}

		MyriadSwordsSyncPayload payload = new MyriadSwordsSyncPayload(snapshots);
		for (ServerPlayer player : level.players()) {
			if (ServerPlayNetworking.canSend(player, MyriadSwordsSyncPayload.TYPE)) {
				ServerPlayNetworking.send(player, payload);
			}
		}
	}

	private static final class SwordPool {
		private final VirtualSword[] swords = new VirtualSword[BASE_POOL_SIZE + EXTRA_POOL_SIZE_PER_LEVEL * 2];
		private int nextId = 1;
		private int activeCount;
		private UUID targetId;
		private int strikeCooldownTicks;

		private VirtualSword acquire(int maxCapacity) {
			int capacity = Math.min(maxCapacity, this.swords.length);
			for (int i = 0; i < capacity; i++) {
				VirtualSword sword = this.swords[i];
				if (sword == null) {
					sword = new VirtualSword(this, this.nextId++, i);
					this.swords[i] = sword;
				}
				if (!sword.active) {
					this.activeCount++;
					return sword;
				}
			}
			return null;
		}

		private void collectActive(ServerLevel level, List<VirtualSword> collector) {
			this.activeCount = 0;
			for (VirtualSword sword : this.swords) {
				if (sword != null && sword.active) {
					if (sword.level == level) {
						collector.add(sword);
					}
					this.activeCount++;
				}
			}
		}

		private void setTarget(Entity target) {
			this.targetId = target.getUUID();
		}

		private boolean hasActiveSwords() {
			for (VirtualSword sword : this.swords) {
				if (sword != null && sword.active) {
					return true;
				}
			}
			return false;
		}

		private void tickTarget(ServerLevel level) {
			if (this.strikeCooldownTicks > 0) {
				this.strikeCooldownTicks--;
			}

			Entity target = getTargetEntity(level);
			if (target == null || !target.isAlive()) {
				this.targetId = null;
			}
		}

		private Entity getTargetEntity(ServerLevel level) {
			return this.targetId == null ? null : level.getEntity(this.targetId);
		}

		private void tryStrikeTarget(ServerLevel level) {
			if (this.targetId == null || this.strikeCooldownTicks > 0) {
				return;
			}

			Entity target = getTargetEntity(level);
			if (target == null || !target.isAlive()) {
				this.targetId = null;
				return;
			}

			List<VirtualSword> participants = new ArrayList<>();
			double hitRange = Math.max(TARGET_HIT_RANGE, target.getBbWidth() * 0.8F);
			double hitRangeSqr = hitRange * hitRange;
			Vec3 targetCenter = target.getBoundingBox().getCenter();
			for (VirtualSword sword : this.swords) {
				if (sword != null && sword.active && sword.level == level) {
					participants.add(sword);
					if (sword.position.distanceToSqr(targetCenter) <= hitRangeSqr) {
						executeStrike(level, target, participants);
						return;
					}
				}
			}

			if (participants.isEmpty()) {
				this.targetId = null;
			}
		}

		private void executeStrike(ServerLevel level, Entity target, List<VirtualSword> swords) {
			if (swords.isEmpty()) {
				this.targetId = null;
				return;
			}

			ServerPlayer owner = swords.get(0).owner(level);
			if (owner == null) {
				this.targetId = null;
				return;
			}

			var damageSource = owner.damageSources().playerAttack(owner);
			float totalDamage = 0.0F;
			for (VirtualSword sword : swords) {
				totalDamage += computeSwordDamage(level, owner, target, sword.visualStack);
			}

			float finalDamage = Math.max(1.0F, totalDamage / 3.0F);
			if (target.hurtServer(level, damageSource, finalDamage)) {
				for (VirtualSword sword : swords) {
					EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, damageSource, sword.visualStack);
					sword.deactivate();
				}
			}

			this.strikeCooldownTicks = STRIKE_COOLDOWN_TICKS;
			this.targetId = null;
		}

		private float computeSwordDamage(ServerLevel level, LivingEntity attacker, Entity target, ItemStack stack) {
			ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
			double baseDamage = modifiers.compute(Attributes.ATTACK_DAMAGE, 0.0D, EquipmentSlot.MAINHAND);
			if (baseDamage <= 0.0D) {
				baseDamage = 1.0D;
			}

			float damage = EnchantmentHelper.modifyDamage(
				level,
				stack,
				target,
				attacker.damageSources().playerAttack((Player) attacker),
				(float) baseDamage
			);
			return Math.max(0.0F, damage);
		}
	}

	private static final class VirtualSword {
		private final SwordPool pool;
		private final int id;
		private final int poolIndex;
		private boolean active;
		private ServerLevel level;
		private UUID ownerId;
		private ItemStack visualStack = ItemStack.EMPTY;
		private Vec3 position = Vec3.ZERO;
		private Vec3 velocity = Vec3.ZERO;
		private Vec3 forward = Vec3.ZERO;
		private int ageTicks;

		private VirtualSword(SwordPool pool, int id, int poolIndex) {
			this.pool = pool;
			this.id = id;
			this.poolIndex = poolIndex;
		}

		private void activate(ServerLevel level, UUID ownerId, ItemStack visualStack, Vec3 position, Vec3 velocity, Vec3 forward) {
			this.active = true;
			this.level = level;
			this.ownerId = ownerId;
			this.visualStack = visualStack;
			this.position = position;
			this.velocity = velocity;
			this.forward = forward.normalize();
			this.ageTicks = 0;
		}

		private Vec3 computeOrbitCenter(ServerPlayer owner) {
			return owner.position().add(0.0D, owner.getBbHeight() + ORBIT_HEIGHT, 0.0D);
		}

		private Vec3 computeOrbitAnchor(ServerPlayer owner) {
			double angle = this.level.getGameTime() * 0.17D + this.poolIndex * (Math.PI * 0.52D);
			double radius = ORBIT_RADIUS + (this.poolIndex % 3) * ORBIT_RADIUS_STEP;
			Vec3 center = computeOrbitCenter(owner);
			double verticalWave = Math.sin((this.level.getGameTime() + this.poolIndex * 5) * 0.16D) * ORBIT_VERTICAL_WAVE;
			return center.add(Math.cos(angle) * radius, verticalWave, Math.sin(angle) * radius);
		}

		private void tick(Vec3 nextVelocity) {
			this.velocity = nextVelocity;
			this.position = this.position.add(this.velocity);
			this.forward = this.velocity.normalize();
			this.ageTicks++;
			if (this.ageTicks >= LIFETIME_TICKS) {
				entityizeAndDrop();
			}
		}

		private void entityizeAndDrop() {
			ThrownTrident thrownTrident = new ThrownTrident(this.level, this.position.x, this.position.y, this.position.z, this.visualStack.copy());
			Vec3 launchVelocity = this.velocity.lengthSqr() > 1.0E-4D ? this.velocity.normalize().scale(Math.max(0.4D, this.velocity.length() * 0.7D)) : new Vec3(0.0D, -0.2D, 0.0D);
			thrownTrident.setDeltaMovement(launchVelocity);
			thrownTrident.pickup = AbstractArrow.Pickup.ALLOWED;
			this.level.addFreshEntity(thrownTrident);
			deactivate();
		}

		private ServerPlayer owner(ServerLevel level) {
			return this.ownerId == null ? null : level.getServer().getPlayerList().getPlayer(this.ownerId);
		}

		private void deactivate() {
			this.active = false;
			this.visualStack = ItemStack.EMPTY;
			this.position = Vec3.ZERO;
			this.velocity = Vec3.ZERO;
			this.forward = Vec3.ZERO;
			this.ownerId = null;
		}
	}
}
