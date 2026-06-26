package com.namith.resonantcaves.client.gui;

import com.namith.resonantcaves.network.payload.CloseScreenPayload;
import com.namith.resonantcaves.network.payload.SimulateDayPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

/**
 * Stats + population-graph screen for the Village Core. Also serves the Creative Village Core
 * (with {@code creative = true}), which shows an extra "Simulate Day" button.
 */
public class VillageCoreScreen extends Screen {
	private static final int PANEL_WIDTH = PopulationGraphPanel.GRAPH_WIDTH + PopulationGraphPanel.MARGIN_LEFT + 16;
	private static final int STATS_HEIGHT = 72;
	private static final int FOOTER_HEIGHT = 70;
	private static final int BACKGROUND_COLOR = 0xFF202020;

	private final BlockPos pos;
	private final boolean creative;
	private final PopulationGraphPanel graph;

	private int villagerCount;
	private int houseCount;
	private long storedEmeralds;

	// Lifetime stats — only set once on open, not updated by periodic push
	private final int piecesPlaced;
	private final long lifetimePillagers;
	private final long deathCount;
	private final long peakPopulation;
	private final long daysActive;

	public VillageCoreScreen(BlockPos pos, int villagerCount, int houseCount, int piecesPlaced,
			long lifetimePillagers, long deathCount, long peakPopulation, long daysActive,
			long storedEmeralds, int[] populationHistory, boolean creative) {
		super(Text.literal(creative ? "Creative Village Core" : "Village Core"));
		this.pos = pos;
		this.villagerCount = villagerCount;
		this.houseCount = houseCount;
		this.storedEmeralds = storedEmeralds;
		this.piecesPlaced = piecesPlaced;
		this.lifetimePillagers = lifetimePillagers;
		this.deathCount = deathCount;
		this.peakPopulation = peakPopulation;
		this.daysActive = daysActive;
		this.creative = creative;
		this.graph = new PopulationGraphPanel(populationHistory);
	}

	public BlockPos getPos() {
		return this.pos;
	}

	public void updateLiveData(int villagerCount, int houseCount, long storedEmeralds) {
		this.villagerCount = villagerCount;
		this.houseCount = houseCount;
		this.storedEmeralds = storedEmeralds;
	}

	@Override
	protected void init() {
		int centerX = this.width / 2;
		int panelTop = panelTop();
		int graphTop = panelTop + STATS_HEIGHT;
		int graphBottom = graphTop + PopulationGraphPanel.GRAPH_HEIGHT;

		// Timeframe buttons sit below the graph (axis day-labels are at graphBottom+4, 8px tall)
		int timeframeY = graphBottom + 16;
		for (ButtonWidget btn : this.graph.createTimeframeButtons(centerX, timeframeY)) {
			this.addDrawableChild(btn);
		}

		int doneY = timeframeY + 18 + 6;
		if (creative) {
			this.addDrawableChild(ButtonWidget.builder(Text.literal("Simulate Day"),
							btn -> ClientPlayNetworking.send(new SimulateDayPayload(pos)))
					.dimensions(centerX - 80, doneY, 100, 20)
					.build());
			this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, btn -> this.close())
					.dimensions(centerX + 26, doneY, 54, 20)
					.build());
		} else {
			this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, btn -> this.close())
					.dimensions(centerX - 50, doneY, 100, 20)
					.build());
		}
	}

	private int panelWidth() {
		return PANEL_WIDTH;
	}

	private int panelHeight() {
		return STATS_HEIGHT + PopulationGraphPanel.GRAPH_HEIGHT + FOOTER_HEIGHT;
	}

	private int panelLeft() {
		return this.width / 2 - panelWidth() / 2;
	}

	private int panelTop() {
		return this.height / 2 - panelHeight() / 2;
	}

	@Override
	public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
		super.renderBackground(context, mouseX, mouseY, delta);
		int pl = panelLeft();
		int pt = panelTop();
		context.fill(pl, pt, pl + panelWidth(), pt + panelHeight(), BACKGROUND_COLOR);
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		int centerX = this.width / 2;
		int pl = panelLeft();
		int pt = panelTop();
		int graphTop = pt + STATS_HEIGHT;

		// Title
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, centerX, pt - 12, 0xFFFFFF);

		// Stats grid (four rows, 12 px apart)
		int row1 = pt + 8;
		int row2 = pt + 20;
		int row3 = pt + 32;
		int row4 = pt + 44;
		int col1 = pl + PopulationGraphPanel.MARGIN_LEFT;
		int col2 = pl + PANEL_WIDTH / 2;

		drawStat(context, col1, row1, "Villagers", String.valueOf(villagerCount), 0xFFFFFF);
		drawStat(context, col2, row1, "Houses", String.valueOf(houseCount), 0xFFFFFF);
		drawStat(context, col1, row2, "Deaths", String.valueOf(deathCount), 0xFF5555);
		drawStat(context, col2, row2, "Pieces placed", String.valueOf(piecesPlaced), 0xAAAAAA);
		drawStat(context, col1, row3, "Peak pop", String.valueOf(peakPopulation), 0xAAAAAA);
		drawStat(context, col2, row3, "Pillagers conv.", String.valueOf(lifetimePillagers), 0xFF9900);
		drawStat(context, col1, row4, "Days active", String.valueOf(daysActive), 0xAAAAAA);

		// Emerald line
		String emeraldText = "Emeralds stored: " + storedEmeralds + "  (right-click to insert / extract 64)";
		context.drawText(this.textRenderer, emeraldText, col1, pt + 58, 0x55FF55, false);

		// Graph
		int graphLeft = pl + PopulationGraphPanel.MARGIN_LEFT;
		this.graph.render(context, this.textRenderer, graphLeft, graphTop);
	}

	private void drawStat(DrawContext context, int x, int y, String label, String value, int valueColor) {
		context.drawText(this.textRenderer, label + ": ", x, y, 0xAAAAAA, false);
		context.drawText(this.textRenderer, value, x + this.textRenderer.getWidth(label + ": "), y, valueColor, false);
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
