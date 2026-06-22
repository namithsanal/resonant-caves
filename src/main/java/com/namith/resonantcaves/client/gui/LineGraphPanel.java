package com.namith.resonantcaves.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A line graph with a 4-button timeframe selector (1h/6h/1d/3d), owned (not extended) by {@code
 * MonitorScreen} and {@code StationScreen} — both screens show a 3,600-sample (3 in-game days at
 * 1 sample/sec) circular history buffer the same way, differing only in what the sampled value
 * means (cable throughput vs. stored energy) and its units label ("RF/t" vs "RF"), which each
 * screen still owns via its own header/readout text. This panel only draws the graph area itself
 * (background, gridlines, time-window labels, peak/zero axis labels, and the data line) and rolls
 * the buffer forward by one sample at a time.
 */
public final class LineGraphPanel {
	public static final int HISTORY_LENGTH = 3600;
	/** Samples per in-game hour (1,000 ticks / 20-tick sample interval). */
	private static final int SAMPLES_PER_HOUR = 50;
	public static final int GRAPH_WIDTH = 280;
	public static final int GRAPH_HEIGHT = 100;
	public static final int MARGIN_LEFT = 50;
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

	private final long[] history;
	private int selectedWindow = HISTORY_LENGTH;

	public LineGraphPanel(long[] initialHistory) {
		this.history = initialHistory.length == HISTORY_LENGTH ? initialHistory.clone() : new long[HISTORY_LENGTH];
	}

	/** Rolls the history window forward by one sample, dropping the oldest. */
	public void appendSample(long newSample) {
		System.arraycopy(this.history, 1, this.history, 0, this.history.length - 1);
		this.history[this.history.length - 1] = newSample;
	}

	/** The most recently appended (or initially seeded) sample. */
	public long getLatestSample() {
		return this.history[HISTORY_LENGTH - 1];
	}

	/**
	 * Builds the 4 timeframe-preset buttons (1h/6h/1d/3d), centered horizontally on {@code
	 * centerX}, for the caller to add via its own (protected) {@code Screen.addDrawableChild}.
	 */
	public List<ButtonWidget> createTimeframeButtons(int centerX, int buttonY) {
		int buttonWidth = 50;
		int totalWidth = buttonWidth * TIMEFRAME_PRESETS.length + 6 * (TIMEFRAME_PRESETS.length - 1);
		int startX = centerX - totalWidth / 2;
		List<ButtonWidget> buttons = new ArrayList<>(TIMEFRAME_PRESETS.length);
		for (int i = 0; i < TIMEFRAME_PRESETS.length; i++) {
			TimeframePreset preset = TIMEFRAME_PRESETS[i];
			buttons.add(ButtonWidget.builder(Text.literal(preset.label), button -> this.selectedWindow = preset.samples)
					.dimensions(startX + i * (buttonWidth + 6), buttonY, buttonWidth, 18)
					.build());
		}
		return buttons;
	}

	/**
	 * Draws the graph background, gridlines/time-window labels, peak/zero axis labels, and the
	 * data line itself, anchored with its top-left corner at {@code (graphLeft, graphTop)}.
	 * {@code unitsLabel} (e.g. {@code "RF/t"} or {@code "RF"}) is appended to the peak-value label.
	 */
	public void render(DrawContext context, TextRenderer textRenderer, int graphLeft, int graphTop, String unitsLabel) {
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
			context.drawText(textRenderer, label, x - textRenderer.getWidth(label) / 2, graphBottom + 4, 0xAAAAAA, false);
		}
		context.drawText(textRenderer, max + " " + unitsLabel + " (peak)", graphLeft, graphTop - 10, 0xAAAAAA, false);
		context.drawText(textRenderer, "0", graphLeft, graphBottom - 8, 0xAAAAAA, false);

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

	private record TimeframePreset(String label, int samples) {
	}
}
