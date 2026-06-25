package com.namith.resonantcaves.client.gui;

import com.namith.resonantcaves.block.entity.VillageCoreBlockEntity;
import com.namith.resonantcaves.network.payload.CloseScreenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * A plain client {@code Screen} (no {@code ScreenHandler}) showing a Village Core's vital
 * statistics — read-only, no controls to set anything, unlike {@code StationScreen}.
 */
public class VillageCoreScreen extends Screen {
	private static final int PANEL_WIDTH = 220;
	private static final int PANEL_HEIGHT = 110;
	private static final int BACKGROUND_COLOR = 0xFF202020;

	private final BlockPos pos;
	private long storedEnergy;
	private int houseCount;
	private int villagerCount;
	private boolean sustained;

	public VillageCoreScreen(BlockPos pos, long storedEnergy, int houseCount, int villagerCount, boolean sustained) {
		super(Text.literal("Village Core"));
		this.pos = pos;
		this.storedEnergy = storedEnergy;
		this.houseCount = houseCount;
		this.villagerCount = villagerCount;
		this.sustained = sustained;
	}

	public BlockPos getPos() {
		return this.pos;
	}

	public void updateState(long storedEnergy, int houseCount, int villagerCount, boolean sustained) {
		this.storedEnergy = storedEnergy;
		this.houseCount = houseCount;
		this.villagerCount = villagerCount;
		this.sustained = sustained;
	}

	private int panelLeft() {
		return this.width / 2 - PANEL_WIDTH / 2;
	}

	private int panelTop() {
		return this.height / 2 - PANEL_HEIGHT / 2;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		int left = this.panelLeft();
		int top = this.panelTop();
		context.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, BACKGROUND_COLOR);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int left = this.panelLeft();
		int top = this.panelTop();
		int centerX = this.width / 2;

		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Village Core"), centerX, top + 8, 0xFFFFFF);
		context.drawText(this.textRenderer, "Houses: " + this.houseCount, left + 12, top + 30, 0xAAAAAA, false);
		context.drawText(this.textRenderer, "Villagers: " + this.villagerCount, left + 12, top + 44, 0xAAAAAA, false);
		context.drawText(this.textRenderer, "Stored: " + this.storedEnergy + " RF", left + 12, top + 58, 0xFFFFFF, false);

		String statusText = this.sustained ? "Status: Sustained" : "Status: Short on power";
		int statusColor = this.sustained ? 0x55FF55 : 0xFF5555;
		context.drawText(this.textRenderer, statusText, left + 12, top + 72, statusColor, false);

		context.drawText(this.textRenderer,
				"Growth: " + this.storedEnergy + " / " + VillageCoreBlockEntity.HOUSE_GROWTH_THRESHOLD + " RF",
				left + 12, top + 86, 0xAAAAAA, false);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void removed() {
		ClientPlayNetworking.send(new CloseScreenPayload(this.pos));
		super.removed();
	}
}
