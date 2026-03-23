package com.fjmode.entity;

import com.fjmode.FJModeMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	private static final Identifier GROUNDED_SWORD_ID = Identifier.fromNamespaceAndPath(FJModeMod.MOD_ID, "grounded_sword");
	public static final ResourceKey<EntityType<?>> GROUNDED_SWORD_KEY = ResourceKey.create(
		BuiltInRegistries.ENTITY_TYPE.key(),
		GROUNDED_SWORD_ID
	);

	public static final EntityType<GroundedSwordEntity> GROUNDED_SWORD = Registry.register(
		BuiltInRegistries.ENTITY_TYPE,
		GROUNDED_SWORD_ID,
		FabricEntityTypeBuilder.<GroundedSwordEntity>create(MobCategory.MISC, GroundedSwordEntity::new)
			.dimensions(EntityDimensions.scalable(0.5F, 0.5F))
			.trackRangeBlocks(32)
			.trackedUpdateRate(20)
			.build(GROUNDED_SWORD_KEY)
	);

	private ModEntities() {
	}

	public static void register() {
	}
}
