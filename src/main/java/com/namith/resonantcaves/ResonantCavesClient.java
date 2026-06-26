package com.namith.resonantcaves;

import com.mojang.blaze3d.systems.RenderSystem;
import com.namith.resonantcaves.client.gui.MonitorScreen;
import com.namith.resonantcaves.client.gui.StationScreen;
import com.namith.resonantcaves.client.gui.VillageCoreScreen;
import com.namith.resonantcaves.item.ModItems;
import com.namith.resonantcaves.network.payload.MonitorHistoryUpdatePayload;
import com.namith.resonantcaves.network.payload.OpenMonitorScreenPayload;
import com.namith.resonantcaves.network.payload.OpenStationScreenPayload;
import com.namith.resonantcaves.network.payload.OpenVillageCoreScreenPayload;
import com.namith.resonantcaves.network.payload.StationStateUpdatePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.Random;

/** Client entrypoint. Hosts the Resonant Helmet's "hostile mob radar" rendering (Feature 7). */
public class ResonantCavesClient implements ClientModInitializer {
	private static final double RADIUS = 128.0;
	// Half-width of the orb quad as a fraction of distance to the camera, so its on-screen size
	// stays constant regardless of how far away the mob is.
	private static final double ANGULAR_SIZE = 0.004;
	private static final int MIN_ALPHA = 38; // ~15% of 255
	private static final int MAX_ALPHA = 200;
	private static final double BREATH_PERIOD_TICKS = 300.0;
	private static final double WANDER_AMPLITUDE = 0.25;
	// The orb fades between a dark and a light shade of the same ender-pearl teal hue (~170°),
	// sampled from the real ender_pearl.png texture (avg RGB ~20,88,77 — matches DARK almost
	// exactly), rather than crossing into a visually distinct hue at the bright end.
	private static final int DARK_RED = 16;
	private static final int DARK_GREEN = 94;
	private static final int DARK_BLUE = 81;
	private static final int LIGHT_RED = 106;
	private static final int LIGHT_GREEN = 235;
	private static final int LIGHT_BLUE = 213;
	private static final Identifier ORB_TEXTURE = Identifier.of("resonantcaves", "textures/misc/radar_orb.png");

	@Override
	public void onInitializeClient() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(ResonantCavesClient::renderRadar);

		// Monitor/station GUIs have no ScreenHandler — these payloads just say which Screen to open
		// (and seed it with the current data) or which already-open screen to push an update into.
		ClientPlayNetworking.registerGlobalReceiver(OpenMonitorScreenPayload.ID, (payload, context) ->
				context.client().execute(() -> context.client().setScreen(new MonitorScreen(
						payload.pos(), payload.tier(), payload.history(), payload.flowDirection()))));

		ClientPlayNetworking.registerGlobalReceiver(MonitorHistoryUpdatePayload.ID, (payload, context) ->
				context.client().execute(() -> {
					if (context.client().currentScreen instanceof MonitorScreen screen && screen.getPos().equals(payload.pos())) {
						screen.appendSample(payload.newSample(), payload.flowDirection());
					}
				}));

		ClientPlayNetworking.registerGlobalReceiver(OpenStationScreenPayload.ID, (payload, context) ->
				context.client().execute(() -> context.client().setScreen(new StationScreen(
						payload.pos(), payload.tier(), payload.creative(), payload.storedEnergy(), payload.targetOutput(), payload.history()))));

		ClientPlayNetworking.registerGlobalReceiver(StationStateUpdatePayload.ID, (payload, context) ->
				context.client().execute(() -> {
					if (context.client().currentScreen instanceof StationScreen screen && screen.getPos().equals(payload.pos())) {
						screen.updateStoredEnergy(payload.storedEnergy(), payload.ratePerTick());
					}
				}));

		ClientPlayNetworking.registerGlobalReceiver(OpenVillageCoreScreenPayload.ID, (payload, context) ->
				context.client().execute(() -> context.client().setScreen(new VillageCoreScreen(payload.pos()))));
	}

	private static void renderRadar(WorldRenderContext context) {
		MinecraftClient client = MinecraftClient.getInstance();
		ClientPlayerEntity player = client.player;
		ClientWorld world = context.world();
		if (player == null || world == null) {
			return;
		}
		if (!player.getEquippedStack(EquipmentSlot.HEAD).isOf(ModItems.RESONANT_HELMET)) {
			return;
		}

		Box searchBox = player.getBoundingBox().expand(RADIUS);
		List<HostileEntity> hostiles = world.getEntitiesByClass(HostileEntity.class, searchBox,
				hostile -> hostile.isAlive() && player.squaredDistanceTo(hostile) <= RADIUS * RADIUS);
		if (hostiles.isEmpty()) {
			return;
		}

		Camera camera = context.camera();
		Vec3d camPos = camera.getPos();
		Quaternionf rotation = new Quaternionf(camera.getRotation());

		int light = LightmapTextureManager.MAX_LIGHT_COORDINATE;
		double tick = world.getTime();

		BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT);
		for (HostileEntity hostile : hostiles) {
			// Per-entity offset and wander shape, seeded from the mob's UUID (not its sequential entity
			// ID) so each orb's breathing/wandering is well out of sync with the others, even for mobs
			// spawned together in the same pack.
			long uuidSeed = hostile.getUuid().getMostSignificantBits() ^ hostile.getUuid().getLeastSignificantBits();
			double offset = Math.abs(uuidSeed % 100000);
			Random random = new Random(uuidSeed);
			double freqX = 0.5 + random.nextDouble();
			double freqY = 0.5 + random.nextDouble();
			double freqZ = 0.5 + random.nextDouble();
			double phaseX = random.nextDouble() * 2.0 * Math.PI;
			double phaseY = random.nextDouble() * 2.0 * Math.PI;
			double phaseZ = random.nextDouble() * 2.0 * Math.PI;

			double breathPhase = ((tick + offset) % BREATH_PERIOD_TICKS) / BREATH_PERIOD_TICKS * 2.0 * Math.PI;
			double pulse = (Math.sin(breathPhase) + 1.0) / 2.0;
			int alpha = (int) (MIN_ALPHA + (MAX_ALPHA - MIN_ALPHA) * pulse);
			int red = (int) (DARK_RED + (LIGHT_RED - DARK_RED) * pulse);
			int green = (int) (DARK_GREEN + (LIGHT_GREEN - DARK_GREEN) * pulse);
			int blue = (int) (DARK_BLUE + (LIGHT_BLUE - DARK_BLUE) * pulse);

			// Wander shares the breathing cycle's period/phase, so each orb returns to center in sync
			// with its own breathing — the per-axis frequency multipliers/phase offsets (randomized
			// per mob above) give each orb its own organic, non-circular wander path within that cycle.
			double wanderX = Math.sin(breathPhase * freqX + phaseX) * WANDER_AMPLITUDE;
			double wanderY = Math.sin(breathPhase * freqY + phaseY) * WANDER_AMPLITUDE;
			double wanderZ = Math.cos(breathPhase * freqZ + phaseZ) * WANDER_AMPLITUDE;

			Vec3d center = hostile.getBoundingBox().getCenter();
			float dx = (float) (center.x + wanderX - camPos.x);
			float dy = (float) (center.y + wanderY - camPos.y);
			float dz = (float) (center.z + wanderZ - camPos.z);
			float size = (float) (Math.sqrt(dx * dx + dy * dy + dz * dz) * ANGULAR_SIZE);
			addQuad(buffer, rotation, dx, dy, dz, size, red, green, blue, alpha, light);
		}

		// getTextSeeThrough's "ALWAYS_DEPTH_TEST" phase is a no-op for GL_ALWAYS (Mojang special-cases
		// depthFunction == 519 to skip touching GL state at all), so it does NOT actually disable depth
		// testing on its own — the world renderer's GL_LEQUAL test against the wall's depth buffer would
		// still occlude the orb. Disable depth testing manually so the orb draws through walls.
		RenderSystem.disableDepthTest();
		RenderLayer.getTextSeeThrough(ORB_TEXTURE).draw(buffer.end());
		RenderSystem.enableDepthTest();
	}

	private static void addQuad(BufferBuilder buffer, Quaternionf rotation, float dx, float dy, float dz, float size, int red, int green, int blue, int alpha, int light) {
		addVertex(buffer, rotation, dx, dy, dz, 1.0F, -1.0F, size, 1.0F, 1.0F, red, green, blue, alpha, light);
		addVertex(buffer, rotation, dx, dy, dz, 1.0F, 1.0F, size, 1.0F, 0.0F, red, green, blue, alpha, light);
		addVertex(buffer, rotation, dx, dy, dz, -1.0F, 1.0F, size, 0.0F, 0.0F, red, green, blue, alpha, light);
		addVertex(buffer, rotation, dx, dy, dz, -1.0F, -1.0F, size, 0.0F, 1.0F, red, green, blue, alpha, light);
	}

	private static void addVertex(BufferBuilder buffer, Quaternionf rotation, float dx, float dy, float dz,
			float localX, float localY, float size, float u, float v, int red, int green, int blue, int alpha, int light) {
		Vector3f pos = new Vector3f(localX, localY, 0.0F).rotate(rotation).mul(size).add(dx, dy, dz);
		buffer.vertex(pos.x(), pos.y(), pos.z())
				.color(red, green, blue, alpha)
				.texture(u, v)
				.light(light);
	}
}
