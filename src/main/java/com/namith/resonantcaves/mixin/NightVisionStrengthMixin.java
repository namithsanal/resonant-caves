package com.namith.resonantcaves.mixin;

import com.namith.resonantcaves.item.ModItems;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Resonant Helmet's Night Vision is intentionally weaker than the vanilla potion: cap the
 * blend strength GameRenderer uses for the night-vision screen effect.
 */
@Mixin(GameRenderer.class)
public class NightVisionStrengthMixin {
	private static final float RESONANT_HELMET_STRENGTH_CAP = 0.1F;

	@Inject(method = "getNightVisionStrength", at = @At("RETURN"), cancellable = true)
	private static void resonantcaves$capStrength(LivingEntity entity, float tickDelta, CallbackInfoReturnable<Float> cir) {
		if (entity.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.RESONANT_HELMET)) {
			cir.setReturnValue(Math.min(cir.getReturnValue(), RESONANT_HELMET_STRENGTH_CAP));
		}
	}
}
