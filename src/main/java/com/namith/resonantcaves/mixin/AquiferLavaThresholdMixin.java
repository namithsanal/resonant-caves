package com.namith.resonantcaves.mixin;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.RandomSplitter;
import net.minecraft.world.gen.chunk.AquiferSampler;
import net.minecraft.world.gen.chunk.ChunkNoiseSampler;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Vanilla's aquifer-to-lava decision hardcodes an absolute Y threshold of -10 (54 blocks above
 * vanilla's min_y of -64) below which fluid pockets have a chance to generate as lava instead of
 * water. Resonant World's floor is at -256, so that same absolute -10 threshold would otherwise
 * cover nearly the entire underground instead of just the deepest sliver — flooding most caves
 * with lava. Capturing the dimension's actual min_y and re-deriving the threshold as
 * "54 blocks above the floor" reproduces vanilla's behavior exactly there (-64 + 54 = -10) while
 * correctly scaling it for any other world height.
 */
@Mixin(AquiferSampler.Impl.class)
public abstract class AquiferLavaThresholdMixin {
	@Unique
	private int resonantcaves$minimumY;

	@Inject(method = "<init>", at = @At("TAIL"))
	private void resonantcaves$captureMinimumY(ChunkNoiseSampler chunkNoiseSampler, ChunkPos chunkPos, NoiseRouter noiseRouter,
			RandomSplitter randomSplitter, int minimumY, int height, AquiferSampler.FluidLevelSampler fluidLevelSampler, CallbackInfo ci) {
		this.resonantcaves$minimumY = minimumY;
	}

	@ModifyConstant(method = "getFluidBlockState", constant = @Constant(intValue = -10))
	private int resonantcaves$scaleLavaThreshold(int original) {
		return this.resonantcaves$minimumY + 54;
	}
}
