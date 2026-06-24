package com.namith.resonantcaves.mixin;

import com.namith.resonantcaves.client.render.feature.ResonantWandFeatureRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.SnowGolemEntityRenderer;
import net.minecraft.client.render.entity.model.SnowGolemEntityModel;
import net.minecraft.entity.passive.SnowGolemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Resonant Wand — adds {@link ResonantWandFeatureRenderer} to the snow golem renderer. */
@Mixin(SnowGolemEntityRenderer.class)
public abstract class SnowGolemRendererMixin extends MobEntityRenderer<SnowGolemEntity, SnowGolemEntityModel<SnowGolemEntity>> {
	public SnowGolemRendererMixin(EntityRendererFactory.Context context, SnowGolemEntityModel<SnowGolemEntity> model, float shadowRadius) {
		super(context, model, shadowRadius);
	}

	@Inject(method = "<init>", at = @At("TAIL"))
	private void resonantcaves$addWandFeature(EntityRendererFactory.Context context, CallbackInfo ci) {
		this.addFeature(new ResonantWandFeatureRenderer(this, context.getItemRenderer()));
	}
}
