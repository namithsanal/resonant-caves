package com.namith.resonantcaves.client.gui;

import java.util.Locale;

import com.namith.resonantcaves.block.EnergyTier;
import com.namith.resonantcaves.network.payload.CloseScreenPayload;
import com.namith.resonantcaves.network.payload.SetStationOutputPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * A plain client {@code Screen} (no {@code ScreenHandler}) showing a station's live stored-energy
 * readout, a line graph of that same stored amount over time (the station's own history, sampled
 * once per second alongside its leak tick — see {@code StationBlockEntity}), and a field for
 * typing a precise target output value. The graph/timeframe-selector here is deliberately the
 * same shape as the Energy Monitor's — folding it directly into this screen, rather than mounting
 * a separate Monitor on the station, avoids a second block doing almost the same job. Reused by
 * the Creative Station with {@code creative = true}, which has no real storage and so skips both
 * the stored-energy line and the graph in favour of an "unlimited" label.
 */
public class StationScreen extends Screen {
	private static final int HISTORY_LENGTH = 3600;
	/** Samples per in-game hour (1,000 ticks / 20-tick sample interval). */
	private static final int SAMPLES_PER_HOUR = 50;
	private static final int GRAPH_WIDTH = 280;
	private static final int GRAPH_HEIGHT = 100;
	private static final int MARGIN_LEFT = 50;
	private static final int HEADER_HEIGHT = 72;
	private static final int FOOTER_HEIGHT = 96;
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
	private final boolean creative;
	private final long[] history;
	private long storedEnergy;
	private long ratePerTick;
	private long targetOutput;
	private int selectedWindow = HISTORY_LENGTH;
	private TextFieldWidget outputField;

	public StationScreen(BlockPos pos, EnergyTier tier, boolean creative, long storedEnergy, long targetOutput, long[] history) {
		super(Text.literal(creative ? "Creative Station" : "Station"));
		this.pos = pos;
		this.tier = tier;
		this.creative = creative;
		this.storedEnergy = storedEnergy;
		this.targetOutput = targetOutput;
		this.history = history.length == HISTORY_LENGTH ? history.clone() : new long[HISTORY_LENGTH];
	}

	public BlockPos getPos() {
		return this.pos;
	}

	/**
	 * {@code ratePerTick} is the net change since the previous push (one second ago), not an
	 * average. {@code storedEnergy} doubles as the graph's newest sample, rolling the history
	 * window forward by one — mirrors {@code MonitorScreen.appendSample}.
	 */
	public void updateStoredEnergy(long storedEnergy, long ratePerTick) {
		this.storedEnergy = storedEnergy;
		this.ratePerTick = ratePerTick;
		System.arraycopy(this.history, 1, this.history, 0, this.history.length - 1);
		this.history[this.history.length - 1] = storedEnergy;
	}

	@Override
	protected void init() {
		if (this.creative) {
			this.initCreative();
		} else {
			this.initFull();
		}
	}

	private void initCreative() {
		int centerX = this.width / 2;
		int top = this.height / 2 - 40;

		this.outputField = new TextFieldWidget(this.textRenderer, centerX - 80, top + 52, 160, 20, Text.literal("Target output"));
		this.outputField.setTextPredicate(text -> text.isEmpty() || text.matches("[0-9]{0,19}"));
		this.outputField.setText(String.valueOf(this.targetOutput));
		this.addDrawableChild(this.outputField);

		this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> {
					this.applyOutput(this.parseField());
					this.close();
				})
				.dimensions(centerX - 50, top + 82, 100, 20)
				.build());
	}

	private void initFull() {
		int panelLeft = this.panelLeft();
		int panelTop = this.panelTop();
		int graphBottom = panelTop + HEADER_HEIGHT + GRAPH_HEIGHT;
		int centerX = this.width / 2;

		int buttonWidth = 50;
		int totalWidth = buttonWidth * TIMEFRAME_PRESETS.length + 6 * (TIMEFRAME_PRESETS.length - 1);
		int startX = centerX - totalWidth / 2;
		int buttonY = panelTop + 22;
		for (int i = 0; i < TIMEFRAME_PRESETS.length; i++) {
			TimeframePreset preset = TIMEFRAME_PRESETS[i];
			this.addDrawableChild(ButtonWidget.builder(Text.literal(preset.label), button -> this.selectedWindow = preset.samples)
					.dimensions(startX + i * (buttonWidth + 6), buttonY, buttonWidth, 18)
					.build());
		}

		this.outputField = new TextFieldWidget(
				this.textRenderer, panelLeft + MARGIN_LEFT, graphBottom + 36, GRAPH_WIDTH - 100, 20, Text.literal("Target output"));
		this.outputField.setTextPredicate(text -> text.isEmpty() || text.matches("[0-9]{0,19}"));
		this.outputField.setText(String.valueOf(this.targetOutput));
		this.addDrawableChild(this.outputField);

		this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> {
					this.applyOutput(this.parseField());
					this.close();
				})
				.dimensions(centerX - 50, graphBottom + 64, 100, 20)
				.build());
	}

	private long parseField() {
		try {
			return Math.max(0, Long.parseLong(this.outputField.getText()));
		} catch (NumberFormatException e) {
			return this.targetOutput;
		}
	}

	private void applyOutput(long value) {
		this.targetOutput = value;
		ClientPlayNetworking.send(new SetStationOutputPayload(this.pos, value));
	}

	private int panelWidth() {
		return this.creative ? 200 : GRAPH_WIDTH + MARGIN_LEFT + 16;
	}

	private int panelHeight() {
		return this.creative ? 122 : HEADER_HEIGHT + GRAPH_HEIGHT + FOOTER_HEIGHT;
	}

	private int panelLeft() {
		return this.width / 2 - this.panelWidth() / 2;
	}

	private int panelTop() {
		return this.height / 2 - this.panelHeight() / 2;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		int panelLeft = this.panelLeft();
		int panelTop = this.panelTop();
		context.fill(panelLeft, panelTop, panelLeft + this.panelWidth(), panelTop + this.panelHeight(), BACKGROUND_COLOR);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		if (this.creative) {
			this.renderCreative(context);
		} else {
			this.renderFull(context);
		}
	}

	private void renderCreative(DrawContext context) {
		int centerX = this.width / 2;
		int top = this.height / 2 - 40;

		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Creative Station"), centerX, top, 0xFFFFFF);
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal("Stored: ∞ (unlimited)"), centerX, top + 14, 0xAAFFAA);
		context.drawText(this.textRenderer, "Target output (RF/t):", centerX - 80, top + 40, 0xAAAAAA, false);
	}

	private void renderFull(DrawContext context) {
		int panelLeft = this.panelLeft();
		int panelTop = this.panelTop();
		int centerX = this.width / 2;

		String title = capitalize(this.tier.name()) + " Station";
		context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(title), centerX, panelTop + 6, 0xFFFFFF);

		int graphLeft = panelLeft + MARGIN_LEFT;
		int graphTop = panelTop + HEADER_HEIGHT;
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
		// Stored energy is an amount (RF), not a throughput rate — unlike the Energy Monitor's
		// cable-throughput graph, this label is intentionally "RF", not "RF/t".
		context.drawText(this.textRenderer, max + " RF (peak)", graphLeft, graphTop - 10, 0xAAAAAA, false);
		context.drawText(this.textRenderer, "0", graphLeft, graphBottom - 8, 0xAAAAAA, false);

		String storedText = "Stored: " + this.storedEnergy + " RF";
		context.drawText(this.textRenderer, storedText, graphRight - this.textRenderer.getWidth(storedText), graphTop - 22, 0xFFFFFF, false);

		String sign = this.ratePerTick > 0 ? "+" : "";
		int rateColor = this.ratePerTick > 0 ? 0x55FF55 : (this.ratePerTick < 0 ? 0xFF5555 : 0xAAAAAA);
		String rateText = "Rate: " + sign + this.ratePerTick + " RF/t";
		context.drawText(this.textRenderer, rateText, graphRight - this.textRenderer.getWidth(rateText), graphTop - 10, rateColor, false);

		// A true line graph: bridge each column to the previous one, since DrawContext has no
		// diagonal line primitive — only horizontal/vertical lines and fills.
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

		context.drawText(this.textRenderer, "Target output (RF/t):", graphLeft, graphBottom + 24, 0xAAAAAA, false);
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
