package com.namith.surfacesurvival.mixin;

import com.namith.surfacesurvival.UnbreakableBlocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes stone and deepslate behave like bedrock when mined: the per-tick breaking delta is forced to
 * zero, so survival players make no progress and no cracking animation is shown. Creative players are
 * unaffected — they break blocks instantly through a separate code path that does not consult this.
 */
@Mixin(AbstractBlock.AbstractBlockState.class)
public class AbstractBlockStateMixin {
	@Inject(method = "calcBlockBreakingDelta", at = @At("HEAD"), cancellable = true)
	private void surfacesurvival$preventMining(PlayerEntity player, BlockView world, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		if (player != null && !player.isCreative() && UnbreakableBlocks.isUnbreakable((BlockState) (Object) this)) {
			cir.setReturnValue(0.0F);
		}
	}
}
