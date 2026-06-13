package com.namith.resonantcaves.mixin;

import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * The Resonant Helmet's Night Vision is applied with showIcon=false so the top-right HUD
 * (InGameHud) hides it, but AbstractInventoryScreen.drawStatusEffects ignores shouldShowIcon
 * and draws every active effect. Filter those out here too, for consistency with the HUD.
 */
@Mixin(AbstractInventoryScreen.class)
public class StatusEffectIconMixin {
	@ModifyVariable(method = "drawStatusEffects", at = @At("STORE"))
	private Collection<StatusEffectInstance> resonantcaves$hideIconlessEffects(Collection<StatusEffectInstance> effects) {
		return effects.stream().filter(StatusEffectInstance::shouldShowIcon).collect(Collectors.toList());
	}
}
