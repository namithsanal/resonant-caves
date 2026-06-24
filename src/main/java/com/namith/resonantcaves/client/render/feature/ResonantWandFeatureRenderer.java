package com.namith.resonantcaves.client.render.feature;

import com.namith.resonantcaves.item.ModItems;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.SnowGolemEntityModel;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.passive.SnowGolemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;

/**
 * Renders the Resonant Wand "held" at a fixed, hand-picked offset near a wand-equipped snow
 * golem's side. Not vanilla's {@code HeldItemFeatureRenderer} (which needs {@code ModelWithArms} —
 * the snow golem model has no arm bone at all), so this is a static cosmetic transform rather than
 * a real arm-driven pose; tuned visually rather than derived. Modeled on
 * {@code SnowGolemPumpkinFeatureRenderer}'s constructor-injection and render-call shape.
 */
@Environment(EnvType.CLIENT)
public class ResonantWandFeatureRenderer extends FeatureRenderer<SnowGolemEntity, SnowGolemEntityModel<SnowGolemEntity>> {
	private final ItemRenderer itemRenderer;

	public ResonantWandFeatureRenderer(FeatureRendererContext<SnowGolemEntity, SnowGolemEntityModel<SnowGolemEntity>> context,
			ItemRenderer itemRenderer) {
		super(context);
		this.itemRenderer = itemRenderer;
	}

	@Override
	public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, SnowGolemEntity golem,
			float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		ItemStack stack = golem.getMainHandStack();
		if (stack.isOf(ModItems.RESONANT_WAND) && !golem.isInvisible()) {
			matrices.push();
			matrices.translate(0.4, 0.6, 0.1);
			matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
			matrices.scale(0.625F, 0.625F, 0.625F);
			this.itemRenderer.renderItem(golem, stack, ModelTransformationMode.THIRD_PERSON_RIGHT_HAND, false,
					matrices, vertexConsumers, golem.getWorld(), light, LivingEntityRenderer.getOverlay(golem, 0.0F), golem.getId());
			matrices.pop();
		}
	}
}
