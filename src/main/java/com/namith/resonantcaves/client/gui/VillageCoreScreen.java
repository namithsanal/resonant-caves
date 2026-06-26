package com.namith.resonantcaves.client.gui;

import com.namith.resonantcaves.network.payload.GenerateVillagePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * GUI for the Creative Village Core. Each click of "Expand" increments the
 * jigsaw depth by 1 and regenerates the village so the settlement grows
 * incrementally. The screen stays open between clicks; press Escape when done.
 * The button label always shows what the NEXT click will produce.
 */
public class VillageCoreScreen extends Screen {
	private static final int PANEL_W = 220;
	private static final int PANEL_H = 68;
	private static final int BACKGROUND_COLOR = 0xFF202020;
	private static final int DEPTH_MAX = 10;

	private final BlockPos pos;
	/** Current depth already generated (0 = nothing generated yet). */
	private int depth;

	public VillageCoreScreen(BlockPos pos, int savedDepth) {
		super(Text.literal("Creative Village Core"));
		this.pos = pos;
		this.depth = savedDepth;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int panelTop = this.height / 2 - PANEL_H / 2;

		String initialLabel = this.depth >= DEPTH_MAX
				? "Max size (" + DEPTH_MAX + ")"
				: "Expand to size " + (this.depth + 1);

		ButtonWidget expandButton = ButtonWidget.builder(Text.literal(initialLabel), button -> {
					this.depth = Math.min(this.depth + 1, DEPTH_MAX);
					ClientPlayNetworking.send(new GenerateVillagePayload(this.pos, this.depth));
					if (this.depth >= DEPTH_MAX) {
						button.setMessage(Text.literal("Max size (" + DEPTH_MAX + ")"));
						button.active = false;
					} else {
						button.setMessage(Text.literal("Expand to size " + (this.depth + 1)));
					}
				})
				.dimensions(centerX - 90, panelTop + 36, 180, 20)
				.build();
		expandButton.active = this.depth < DEPTH_MAX;
		this.addDrawableChild(expandButton);
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		int panelLeft = this.width / 2 - PANEL_W / 2;
		int panelTop = this.height / 2 - PANEL_H / 2;
		context.fill(panelLeft, panelTop, panelLeft + PANEL_W, panelTop + PANEL_H, BACKGROUND_COLOR);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Creative Village Core"),
				this.width / 2, this.height / 2 - PANEL_H / 2 + 14, 0xFFFFFF);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
