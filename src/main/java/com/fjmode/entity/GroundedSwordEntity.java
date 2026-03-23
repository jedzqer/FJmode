package com.fjmode.entity;

import com.mojang.serialization.Codec;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GroundedSwordEntity extends AbstractArrow {
	private static final EntityDataAccessor<ItemStack> DATA_SWORD_STACK = SynchedEntityData.defineId(GroundedSwordEntity.class, EntityDataSerializers.ITEM_STACK);
	private static final EntityDataAccessor<Boolean> DATA_EMBEDDED = SynchedEntityData.defineId(GroundedSwordEntity.class, EntityDataSerializers.BOOLEAN);
	private ItemStack swordStack = ItemStack.EMPTY;
	private boolean embedded;

	public GroundedSwordEntity(EntityType<? extends GroundedSwordEntity> entityType, Level level) {
		super(entityType, level);
		this.pickup = Pickup.ALLOWED;
		this.setBaseDamage(0.0D);
	}

	public GroundedSwordEntity(Level level, double x, double y, double z, ItemStack swordStack) {
		this(ModEntities.GROUNDED_SWORD, level);
		this.setPos(x, y, z);
		this.setSwordStack(swordStack == null ? ItemStack.EMPTY : swordStack.copyWithCount(1));
	}

	public ItemStack getSwordStack() {
		ItemStack syncedStack = this.getEntityData().get(DATA_SWORD_STACK);
		if (syncedStack != null && !syncedStack.isEmpty()) {
			return syncedStack.copy();
		}
		return this.swordStack == null || this.swordStack.isEmpty() ? ItemStack.EMPTY : this.swordStack.copy();
	}

	private void setSwordStack(ItemStack swordStack) {
		this.swordStack = swordStack == null ? ItemStack.EMPTY : swordStack.copyWithCount(1);
		this.getEntityData().set(DATA_SWORD_STACK, this.swordStack.copy());
		this.setPickupItemStack(this.swordStack.copy());
	}

	public void embedAt(Vec3 position, Vec3 direction) {
		Vec3 facing = direction.lengthSqr() > 1.0E-4D ? direction.normalize() : new Vec3(0.0D, -1.0D, 0.0D);
		this.setPos(position.x, position.y, position.z);
		this.setDeltaMovement(Vec3.ZERO);
		this.setNoGravity(true);
		this.setInGround(true);
		this.inGroundTime = 0;
		this.shakeTime = 0;
		this.setYRot((float) Math.toDegrees(Math.atan2(facing.x, facing.z)));
		this.setXRot((float) Math.toDegrees(Math.atan2(facing.y, Math.sqrt(facing.x * facing.x + facing.z * facing.z))));
		this.xRotO = this.getXRot();
		this.yRotO = this.getYRot();
		this.embedded = true;
		this.getEntityData().set(DATA_EMBEDDED, true);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SWORD_STACK, ItemStack.EMPTY);
		builder.define(DATA_EMBEDDED, false);
	}

	@Override
	protected boolean canHitEntity(Entity entity) {
		return false;
	}

	@Override
	protected ItemStack getDefaultPickupItem() {
		return this.swordStack == null || this.swordStack.isEmpty() ? ItemStack.EMPTY : this.swordStack.copy();
	}

	@Override
	public void addAdditionalSaveData(ValueOutput valueOutput) {
		super.addAdditionalSaveData(valueOutput);
		valueOutput.store("Embedded", Codec.BOOL, this.embedded);
	}

	@Override
	public void readAdditionalSaveData(ValueInput valueInput) {
		super.readAdditionalSaveData(valueInput);
		ItemStack pickupStack = this.getPickupItemStackOrigin();
		if (pickupStack != null && !pickupStack.isEmpty()) {
			this.setSwordStack(pickupStack);
		}
		this.embedded = valueInput.read("Embedded", Codec.BOOL).orElse(false);
		this.getEntityData().set(DATA_EMBEDDED, this.embedded);
		if (this.embedded) {
			this.setDeltaMovement(Vec3.ZERO);
			this.setNoGravity(true);
			this.setInGround(true);
			this.inGroundTime = 0;
			this.shakeTime = 0;
			this.xRotO = this.getXRot();
			this.yRotO = this.getYRot();
		}
	}

	@Override
	protected void onHitBlock(BlockHitResult blockHitResult) {
		super.onHitBlock(blockHitResult);
		Vec3 travelDirection = this.getDeltaMovement();
		Vec3 embedOffset = travelDirection.lengthSqr() > 1.0E-4D
			? travelDirection.normalize().scale(0.12D)
			: new Vec3(0.0D, -0.12D, 0.0D);
		this.embedAt(blockHitResult.getLocation().subtract(embedOffset), travelDirection);
	}

	@Override
	protected SoundEvent getDefaultHitGroundSoundEvent() {
		return SoundEvents.TRIDENT_HIT_GROUND;
	}

	@Override
	public void tick() {
		if (this.getEntityData().get(DATA_EMBEDDED)) {
			this.embedded = true;
			this.setInGround(true);
			this.setDeltaMovement(Vec3.ZERO);
			this.setNoGravity(true);
			this.shakeTime = 0;
			return;
		}
		super.tick();
	}

	@Override
	protected void tickDespawn() {
		if (this.getEntityData().get(DATA_EMBEDDED)) {
			return;
		}
		super.tickDespawn();
	}

	@Override
	public void playerTouch(Player player) {
		if (!this.level().isClientSide()) {
			this.setSwordStack(this.getSwordStack());
		}
		super.playerTouch(player);
	}
}
