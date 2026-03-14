package com.fjmode.mixin.client;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntityRenderer.class)
public interface LivingEntityRendererAccessor {
	@Invoker("addLayer")
	boolean fjmode$invokeAddLayer(RenderLayer<?, ?> renderLayer);

	@Accessor("itemModelResolver")
	ItemModelResolver fjmode$getItemModelResolver();
}
