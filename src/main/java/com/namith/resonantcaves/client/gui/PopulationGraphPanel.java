package com.namith.resonantcaves.client.gui;

import java.util.ArrayList;
import java.util.List;

import com.namith.resonantcaves.block.entity.PopulationHistory;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * A line graph with 3d/7d/28d/Max timeframe buttons showing village population over time. Each
 * sample is one Minecraft day (one {@code processMidnight()} call). NOT based on
 * {@link LineGraphPanel} — that panel is per-second energy data, whereas this is per-day population
 * data, so the timeframe semantics and axis labels are different.
 */
public final class PopulationGraphPanel {
	public static final int GRAPH_WIDTH = 280;
	public static final int GRAPH_HEIGHT = 80;
	public static final int MARGIN_LEFT = 40;
	private static final int BACKGROUND_COLOR = 0xFF1B1B1B;
	private static final int GRID_COLOR = 0xFF3A3A3A;
	private static final int LINE_COLOR = 0xFF55AA55;

	private static final int[] WINDOW_SIZES = {3, 7, 28, PopulationHistory.MAX_DAYS};
	private static final String[] WINDOW_LABELS = {"3d", "7d", "28d", "Max"};

	private int[] history;
	private int selectedWindow = PopulationHistory.MAX_DAYS;

	public PopulationGraphPanel(int[] initialHistory) {
		this.history = initialHistory.clone();
	}

	public void appendSample(int population) {
		int[] newHistory = new int[this.history.length + 1];
		System.arraycopy(this.history, 0, newHistory, 0, this.history.length);
		newHistory[this.history.length] = population;
		// Trim to MAX_DAYS
		if (newHistory.length > PopulationHistory.MAX_DAYS) {
			this.history = new int[PopulationHistory.MAX_DAYS];
			System.arraycopy(newHistory, newHistory.length - PopulationHistory.MAX_DAYS, this.history, 0, PopulationHistory.MAX_DAYS);
		} else {
			this.history = newHistory;
		}
	}

	public List<ButtonWidget> createTimeframeButtons(int centerX, int buttonY) {
		int buttonWidth = 50;
		int gap = 6;
		int totalWidth = buttonWidth * WINDOW_LABELS.length + gap * (WINDOW_LABELS.length - 1);
		int startX = centerX - totalWidth / 2;
		List<ButtonWidget> buttons = new ArrayList<>(WINDOW_LABELS.length);
		for (int i = 0; i < WINDOW_LABELS.length; i++) {
			final int window = WINDOW_SIZES[i];
			buttons.add(ButtonWidget.builder(Text.literal(WINDOW_LABELS[i]), btn -> this.selectedWindow = window)
					.dimensions(startX + i * (buttonWidth + gap), buttonY, buttonWidth, 18)
					.build());
		}
		return buttons;
	}

	public void render(DrawContext context, TextRenderer textRenderer, int graphLeft, int graphTop) {
		int graphRight = graphLeft + GRAPH_WIDTH;
		int graphBottom = graphTop + GRAPH_HEIGHT;
		context.fill(graphLeft, graphTop, graphRight, graphBottom, BACKGROUND_COLOR);

		int count = this.history.length;
		int window = Math.min(this.selectedWindow, count);
		if (window <= 0) {
			context.drawText(textRenderer, "No data yet", graphLeft + 8, graphTop + GRAPH_HEIGHT / 2 - 4, 0x888888, false);
			return;
		}
		int startIndex = count - window;

		int max = 1;
		for (int i = startIndex; i < count; i++) {
			if (history[i] > max) max = history[i];
		}

		// Gridlines
		for (int tick = 0; tick <= 3; tick++) {
			int x = graphLeft + GRAPH_WIDTH * tick / 3;
			context.drawVerticalLine(x, graphTop, graphBottom, GRID_COLOR);
			String label = tick == 3 ? "now" : "-" + (window * (3 - tick) / 3) + "d";
			context.drawText(textRenderer, label, x - textRenderer.getWidth(label) / 2, graphBottom + 4, 0xAAAAAA, false);
		}

		context.drawText(textRenderer, max + " (peak)", graphLeft + 2, graphTop + 2, 0xAAAAAA, false);
		context.drawText(textRenderer, "0", graphLeft, graphBottom - 8, 0xAAAAAA, false);

		// Data line
		int previousY = graphBottom;
		for (int x = 0; x < GRAPH_WIDTH; x++) {
			int sampleIndex = startIndex + x * window / GRAPH_WIDTH;
			int value = this.history[Math.min(sampleIndex, count - 1)];
			int height = (int) Math.round(value * (double) GRAPH_HEIGHT / max);
			int y = graphBottom - height;
			int columnX = graphLeft + x;
			context.drawVerticalLine(columnX, Math.min(previousY, y), Math.max(previousY, y), LINE_COLOR);
			previousY = y;
		}
	}
}
