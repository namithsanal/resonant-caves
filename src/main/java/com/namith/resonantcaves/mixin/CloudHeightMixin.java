package com.namith.resonantcaves.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Resonant World's build envelope is shifted to -256..256 (vanilla is -64..320), so vanilla's
 * fixed cloud height (192, 128 below vanilla's build cap) needs to move with it. Both presets
 * share the "minecraft:overworld" DimensionEffects instance, so this checks the loaded world's
 * actual dimension type rather than DimensionEffects identity, leaving Vanilla World untouched.
 */
@Mixin(DimensionEffects.class)
public class CloudHeightMixin {
	private static final float RESONANT_CLOUDS_HEIGHT = 128.0F;
	private static final int RESONANT_MIN_Y = -256;

	@Inject(method = "getCloudsHeight", at = @At("RETURN"), cancellable = true)
	private void resonantcaves$raiseCloudsHeight(CallbackInfoReturnable<Float> cir) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world != null && world.getDimension().minY() == RESONANT_MIN_Y) {
			cir.setReturnValue(RESONANT_CLOUDS_HEIGHT);
		}
	}
}
