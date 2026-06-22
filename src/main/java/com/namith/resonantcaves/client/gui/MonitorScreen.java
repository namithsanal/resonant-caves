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
 * line graph of an energy monitor's adjacent cable throughput. History arrives as a full
 * chronological snapshot on open ({@link #history}), then one new sample at a time via
 * {@link #appendSample}, rolling the window forward each second. The displayed time window is a
 * manual preset picked via {@link #TIMEFRAME_PRESETS} buttons, not auto-detected from how much
 * history actually exists — an earlier auto-shrinking version didn't read reliably in practice.
 */
public class MonitorScreen extends Screen {
	private static final int HISTORY_LENGTH = 3600;
	/** Samples per in-game hour (1,000 ticks / 20-tick sample interval). */
	private static final int SAMPLES_PER_HOUR = 50;
	private static final int GRAPH_WIDTH = 280;
	private static final int GRAPH_HEIGHT = 100;
	private static final int MARGIN_LEFT = 50;
	private static final int MARGIN_TOP = 72;
	private static final int BACKGROUND_COLOR = 0xFF202020;
	private static final int GRAPH_BACKGROUND_COLOR = 0xFF1B1B1B;
	private static final int GRID_COLOR = 0xFF3A3A3A;
	private static final int LINE_COLOR = 0xFF55FFAA;

	/** Label and window length (in 1-sample-per-second units) for each timeframe preset button. */
	private static final TimeframePreset[] TIMEFRAME_PRESETS = {
			new TimeframePreset("1h", SAMPLES_PER_HOUR),
			new TimeframePreset("6h", SAMPLES_PER_HOUR * 6),
			new TimeframePreset("1d", SAMPLES_PER_HOUR * 24),
			new TimeframePreset("3d", HISTORY_LENGTH),
	};

	private final BlockPos pos;
	private final EnergyTier tier;
	private final long[] history;
	@Nullable
	private Direction flowDirection;
	private int selectedWindow = HISTORY_LENGTH;

	public MonitorScreen(BlockPos pos, EnergyTier tier, long[] history, @Nullable Direction flowDirection) {
		super(Text.literal("Energy Monitor"));
		this.pos = pos;
		this.tier = tier;
		this.history = history.length == HISTORY_LENGTH ? history.clone() : new long[HISTORY_LENGTH];
		this.flowDirection = flowDirection;
	}

	public BlockPos getPos() {
		return this.pos;
	}

	/** Rolls the history window forward by one sample, dropping the oldest. */
	public void appendSample(long newSample, @Nullable Direction flowDirection) {
		System.arraycopy(this.history, 1, this.history, 0, this.history.length - 1);
		this.history[this.history.length - 1] = newSample;
		this.flowDirection = flowDirection;
	}

	@Override
	protected void init() {
		int buttonWidth = 50;
		int totalWidth = buttonWidth * TIMEFRAME_PRESETS.length + 6 * (TIMEFRAME_PRESETS.length - 1);
		int startX = this.width / 2 - totalWidth / 2;
		int buttonY = this.height / 2 - MARGIN_TOP + 22;
		for (int i = 0; i < TIMEFRAME_PRESETS.length; i++) {
			TimeframePreset preset = TIMEFRAME_PRESETS[i];
			this.addDrawableChild(ButtonWidget.builder(Text.literal(preset.label), button -> this.selectedWindow = preset.samples)
					.dimensions(startX + i * (buttonWidth + 6), buttonY, buttonWidth, 18)
					.build());
		}

		this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.close())
				.dimensions(this.width / 2 - 50, this.height / 2 + GRAPH_HEIGHT + 24, 100, 20)
				.build());
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		int panelLeft = this.width / 2 - GRAPH_WIDTH / 2 - MARGIN_LEFT;
		int panelTop = this.height / 2 - MARGIN_TOP;
		int panelWidth = GRAPH_WIDTH + MARGIN_LEFT + 16;
		int panelHeight = MARGIN_TOP + GRAPH_HEIGHT + 54;
		context.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, BACKGROUND_COLOR);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);

		int panelLeft = this.width / 2 - GRAPH_WIDTH / 2 - MARGIN_LEFT;
		int panelTop = this.height / 2 - MARGIN_TOP;

		context.drawCenteredTextWithShadow(this.textRenderer,
				Text.literal("Energy Monitor (" + capitalize(this.tier.name()) + ")"),
				this.width / 2, panelTop + 6, 0xFFFFFF);

		int graphLeft = panelLeft + MARGIN_LEFT;
		int graphTop = panelTop + MARGIN_TOP;
		int graphRight = graphLeft + GRAPH_WIDTH;
		int graphBottom = graphTop + GRAPH_HEIGHT;
		context.fill(graphLeft, graphTop, graphRight, graphBottom, GRAPH_BACKGROUND_COLOR);

		int windowLength = this.selectedWindow;
		int startIndex = HISTORY_LENGTH - windowLength;

		long max = 1;
		for (int i = startIndex; i < HISTORY_LENGTH; i++) {
			max = Math.max(max, this.history[i]);
		}

		// Evenly spaced gridlines across the selected window, oldest on the left to "now" on the right.
		for (int tick = 0; tick <= 3; tick++) {
			int x = graphLeft + (int) ((long) GRAPH_WIDTH * tick / 3);
			context.drawVerticalLine(x, graphTop, graphBottom, GRID_COLOR);
			String label = tick == 3 ? "now" : "-" + formatDuration((long) windowLength * (3 - tick) / 3);
			context.drawText(this.textRenderer, label, x - this.textRenderer.getWidth(label) / 2, graphBottom + 4, 0xAAAAAA, false);
		}
		context.drawText(this.textRenderer, max + " RF/t (peak)", graphLeft, graphTop - 10, 0xAAAAAA, false);
		context.drawText(this.textRenderer, "0", graphLeft, graphBottom - 8, 0xAAAAAA, false);

		// The most recent sample, not a windowed average — matches what the ambient block face shows.
		long current = this.history[HISTORY_LENGTH - 1];
		String currentLabel = "Current: " + current + " RF/t";
		context.drawText(this.textRenderer, currentLabel, graphRight - this.textRenderer.getWidth(currentLabel), graphTop - 22, 0xFFFFFF, false);

		String directionLabel = "Flow: " + (this.flowDirection == null ? "n/a" : arrowFor(this.flowDirection) + " " + capitalize(this.flowDirection.name()));
		context.drawText(this.textRenderer, directionLabel, graphRight - this.textRenderer.getWidth(directionLabel), graphTop - 10, 0xAAAAAA, false);

		// A true line graph: for each column, bridge the previous column's height to this one's,
		// since DrawContext has no diagonal line primitive — only horizontal/vertical lines and fills.
		int previousY = graphBottom;
		for (int x = 0; x < GRAPH_WIDTH; x++) {
			int sampleIndex = startIndex + x * windowLength / GRAPH_WIDTH;
			long value = this.history[Math.min(sampleIndex, HISTORY_LENGTH - 1)];
			int height = (int) Math.round(value * (double) GRAPH_HEIGHT / max);
			int y = graphBottom - height;
			int columnX = graphLeft + x;
			context.drawVerticalLine(columnX, Math.min(previousY, y), Math.max(previousY, y), LINE_COLOR);
			previousY = y;
		}
	}

	/** 1 sample = 1 real-time second; this formats the elapsed window using whichever unit reads best at that scale. */
	private static String formatDuration(long samples) {
		if (samples >= SAMPLES_PER_HOUR) {
			return (samples / SAMPLES_PER_HOUR) + "h";
		}
		if (samples >= 60) {
			return (samples / 60) + "m";
		}
		return samples + "s";
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

	private record TimeframePreset(String label, int samples) {
	}
}
