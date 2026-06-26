package com.namith.resonantcaves.client.gui;

import com.namith.resonantcaves.network.payload.GenerateVillagePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/** Simple GUI for the Creative Village Core — one "Generate" button triggers server-side village placement. */
public class VillageCoreScreen extends Screen {
	private static final int PANEL_W = 200;
	private static final int PANEL_H = 80;
	private static final int BACKGROUND_COLOR = 0xFF202020;

	private final BlockPos pos;

	public VillageCoreScreen(BlockPos pos) {
		super(Text.literal("Creative Village Core"));
		this.pos = pos;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int panelTop = this.height / 2 - PANEL_H / 2;

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Generate"), button -> {
					ClientPlayNetworking.send(new GenerateVillagePayload(this.pos));
					this.close();
				})
				.dimensions(centerX - 60, panelTop + 44, 120, 20)
				.build());
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
				this.width / 2, this.height / 2 - PANEL_H / 2 + 12, 0xFFFFFF);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
