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
import org.jetbrains.annotations.Nullable;

/**
 * A plain client {@code Screen} (no {@code ScreenHandler}) showing a station's live stored-energy
 * readout, a line graph of that same stored amount over time (drawn via the shared {@link
 * LineGraphPanel}, also used by {@code MonitorScreen} — the station's own history, sampled once
 * per second alongside its leak tick, see {@code StationBlockEntity}), and a field for typing a
 * precise target output value. Folding the graph directly into this screen, rather than mounting
 * a separate Monitor on the station, avoids a second block doing almost the same job. Reused by
 * the Creative Station with {@code creative = true}, which has no real storage and so skips both
 * the stored-energy line and the graph in favour of an "unlimited" label; {@code tier} is {@code
 * null} in that case too, since the Creative Station is deliberately untiered.
 */
public class StationScreen extends Screen {
	private static final int HEADER_HEIGHT = 72;
	private static final int FOOTER_HEIGHT = 96;
	private static final int BACKGROUND_COLOR = 0xFF202020;

	private final BlockPos pos;
	@Nullable
	private final EnergyTier tier;
	private final boolean creative;
	private final LineGraphPanel graph;
	private long storedEnergy;
	private long ratePerTick;
	private long targetOutput;
	private TextFieldWidget outputField;

	public StationScreen(BlockPos pos, @Nullable EnergyTier tier, boolean creative, long storedEnergy, long targetOutput, long[] history) {
		super(Text.literal(creative ? "Creative Station" : "Station"));
		this.pos = pos;
		this.tier = tier;
		this.creative = creative;
		this.storedEnergy = storedEnergy;
		this.targetOutput = targetOutput;
		this.graph = new LineGraphPanel(history);
	}

	public BlockPos getPos() {
		return this.pos;
	}

	/**
	 * {@code ratePerTick} is the net change since the previous push (one second ago), not an
	 * average. {@code storedEnergy} doubles as the graph's newest sample.
	 */
	public void updateStoredEnergy(long storedEnergy, long ratePerTick) {
		this.storedEnergy = storedEnergy;
		this.ratePerTick = ratePerTick;
		this.graph.appendSample(storedEnergy);
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
		int graphBottom = panelTop + HEADER_HEIGHT + LineGraphPanel.GRAPH_HEIGHT;
		int centerX = this.width / 2;

		for (ButtonWidget button : this.graph.createTimeframeButtons(centerX, panelTop + 22)) {
			this.addDrawableChild(button);
		}

		this.outputField = new TextFieldWidget(
				this.textRenderer, panelLeft + LineGraphPanel.MARGIN_LEFT, graphBottom + 36, LineGraphPanel.GRAPH_WIDTH - 100, 20, Text.literal("Target output"));
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
		return this.creative ? 200 : LineGraphPanel.GRAPH_WIDTH + LineGraphPanel.MARGIN_LEFT + 16;
	}

	private int panelHeight() {
		return this.creative ? 122 : HEADER_HEIGHT + LineGraphPanel.GRAPH_HEIGHT + FOOTER_HEIGHT;
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

		int graphLeft = panelLeft + LineGraphPanel.MARGIN_LEFT;
		int graphTop = panelTop + HEADER_HEIGHT;
		int graphRight = graphLeft + LineGraphPanel.GRAPH_WIDTH;

		// Stored energy is an amount (RF), not a throughput rate — unlike the Energy Monitor's
		// cable-throughput graph, this units label is intentionally "RF", not "RF/t".
		this.graph.render(context, this.textRenderer, graphLeft, graphTop, "RF");

		String storedText = "Stored: " + this.storedEnergy + " RF";
		context.drawText(this.textRenderer, storedText, graphRight - this.textRenderer.getWidth(storedText), graphTop - 22, 0xFFFFFF, false);

		String sign = this.ratePerTick > 0 ? "+" : "";
		int rateColor = this.ratePerTick > 0 ? 0x55FF55 : (this.ratePerTick < 0 ? 0xFF5555 : 0xAAAAAA);
		String rateText = "Rate: " + sign + this.ratePerTick + " RF/t";
		context.drawText(this.textRenderer, rateText, graphRight - this.textRenderer.getWidth(rateText), graphTop - 10, rateColor, false);

		int graphBottom = graphTop + LineGraphPanel.GRAPH_HEIGHT;
		context.drawText(this.textRenderer, "Target output (RF/t):", graphLeft, graphBottom + 24, 0xAAAAAA, false);
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
