package com.namith.resonantcaves.mixin;

import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * The real source of Resonant World's lava-flooded caves: {@code NoiseChunkGenerator}'s default
 * fluid sampler hardcodes "below absolute Y -54, default to lava" (vanilla: 10 blocks above its
 * min_y of -64). That single constant decides the base fluid for the *entire* region below it,
 * before any of AquiferSampler's pocket-by-pocket blending runs. Resonant World's floor is -256,
 * so the unmodified -54 threshold covered 202 of 256 blocks below sea level instead of the
 * intended thin sliver near the floor. Re-deriving it as "10 blocks above the dimension's actual
 * min_y" reproduces vanilla exactly (-64 + 10 = -54) while scaling correctly elsewhere.
 */
@Mixin(NoiseChunkGenerator.class)
public class NoiseChunkGeneratorLavaLevelMixin {
	@ModifyConstant(method = "createFluidLevelSampler", constant = @Constant(intValue = -54))
	private static int resonantcaves$scaleLavaSeaLevel(int original, ChunkGeneratorSettings settings) {
		return settings.generationShapeConfig().minimumY() + 10;
	}
}
