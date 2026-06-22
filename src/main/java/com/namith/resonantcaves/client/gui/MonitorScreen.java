package com.namith.resonantcaves.client.gui;

import java.util.Locale;

import com.namith.resonantcaves.block.EnergyTier;
import com.namith.resonantcaves.network.payload.CloseScreenPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * A plain client {@code Screen} (no {@code ScreenHandler} — there are no item slots) showing a
 * line graph of an energy monitor's adjacent cable throughput, drawn via the shared {@link
 * LineGraphPanel} (also used by {@code StationScreen}). History arrives as a full chronological
 * snapshot on open, then one new sample at a time via {@link #appendSample}.
 */
public class MonitorScreen extends Screen {
	private static final int MARGIN_TOP = 72;
	private static final int BACKGROUND_COLOR = 0xFF202020;

	private final BlockPos pos;
	private final EnergyTier tier;
	private final LineGraphPanel graph;
	@Nullable
	private Direction flowDirection;

	public MonitorScreen(BlockPos pos, EnergyTier tier, long[] history, @Nullable Direction flowDirection) {
		super(Text.literal("Energy Monitor"));
		this.pos = pos;
		this.tier = tier;
		this.graph = new LineGraphPanel(history);
		this.flowDirection = flowDirection;
	}

	public BlockPos getPos() {
		return this.pos;
	}

	/** Rolls the history window forward by one sample, dropping the oldest. */
	public void appendSample(long newSample, @Nullable Direction flowDirection) {
		this.graph.appendSample(newSample);
		this.flowDirection = flowDirection;
	}

	@Override
	protected void init() {
		int buttonY = this.height / 2 - MARGIN_TOP + 22;
		for (ButtonWidget button : this.graph.createTimeframeButtons(this.width / 2, buttonY)) {
			this.addDrawableChild(button);
		}

		this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close())
				.dimensions(this.width / 2 - 50, this.height / 2 + LineGraphPanel.GRAPH_HEIGHT + 24, 100, 20)
				.build());
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		int panelLeft = this.width / 2 - LineGraphPanel.GRAPH_WIDTH / 2 - LineGraphPanel.MARGIN_LEFT;
		int panelTop = this.height / 2 - MARGIN_TOP;
		int panelWidth = LineGraphPanel.GRAPH_WIDTH + LineGraphPanel.MARGIN_LEFT + 16;
		int panelHeight = MARGIN_TOP + LineGraphPanel.GRAPH_HEIGHT + 54;
		context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, BACKGROUND_COLOR);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		int panelLeft = this.width / 2 - LineGraphPanel.GRAPH_WIDTH / 2 - LineGraphPanel.MARGIN_LEFT;
		int panelTop = this.height / 2 - MARGIN_TOP;

		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("Energy Monitor (" + capitalize(this.tier.name()) + ")"),
				this.width / 2, panelTop + 6, 0xFFFFFF);

		int graphLeft = panelLeft + LineGraphPanel.MARGIN_LEFT;
		int graphTop = panelTop + MARGIN_TOP;
		int graphRight = graphLeft + LineGraphPanel.GRAPH_WIDTH;

		this.graph.render(context, this.textRenderer, graphLeft, graphTop, "RF/t");

		// The most recent sample, not a windowed average — matches what the ambient block face shows.
		long current = this.graph.getLatestSample();
		String currentLabel = "Current: " + current + " RF/t";
		context.drawText(this.textRenderer, currentLabel, graphRight - this.textRenderer.getWidth(currentLabel), graphTop - 22, 0xFFFFFF, false);

		String directionLabel = "Flow: " + (this.flowDirection == null ? "n/a" : arrowFor(this.flowDirection) + " " + capitalize(this.flowDirection.name()));
		context.drawText(this.textRenderer, directionLabel, graphRight - this.textRenderer.getWidth(directionLabel), graphTop - 10, 0xAAAAAA, false);
	}

	private static String arrowFor(Direction direction) {
		return switch (direction) {
			case NORTH -> "↑";
			case SOUTH -> "↓";
			case EAST -> "→";
			case WEST -> "←";
			case UP -> "▲";
			case DOWN -> "▼";
		};
	}

	private static String capitalize(String value) {
		return value.isEmpty() ? value : value.charAt(0) + value.substring(1).toLowerCase(Locale.ROOT);
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
