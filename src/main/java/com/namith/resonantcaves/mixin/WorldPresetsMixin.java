package com.namith.resonantcaves.mixin;

import com.namith.resonantcaves.ResonantCaves;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

/**
 * Makes "Resonant World" the pre-selected preset on the world-creation screen. {@code DEFAULT} is
 * read fresh at each call site (it's assigned via a method call, not a compile-time constant), so
 * mutating it here is sufficient without touching the screen code that reads it.
 */
@Mixin(WorldPresets.class)
public class WorldPresetsMixin {
	@Mutable
	@Shadow
	@Final
	public static RegistryKey<WorldPreset> DEFAULT;

	static {
		DEFAULT = RegistryKey.of(RegistryKeys.WORLD_PRESET, Identifier.of(ResonantCaves.MOD_ID, "resonant_world"));
	}
}
