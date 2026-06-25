# Resonant Caves

A Fabric mod for Minecraft **1.21.1** that overhauls survival around one core idea: **mining is impossible**. Stone and deepslate are unbreakable, so the game shifts from digging to exploration, hunting, and infrastructure.

## Features

1. **Unbreakable stone & deepslate** — `minecraft:stone` and `minecraft:deepslate` behave like bedrock (no mining animation, no breaking in survival). All other blocks — stone variants, dirt, gravel, and every ore — stay breakable, so resources are only accessible where caves expose them.
2. **Resonant Ore** — A naturally-occurring, bedrock-hard ore (with a deepslate variant) that passively generates a small, randomized amount of RF via the TeamReborn Energy API. Found underground at roughly iron ore's rarity, glowing with a faint light and a coral/salmon-tinted texture.
3. **Two world presets** — When creating a new world, choose between **Resonant World** (the default) and **Vanilla World**. Resonant World re-anchors the overworld to a symmetric coordinate system (sea level 0, world bottom Y-256, build cap Y256) instead of vanilla's (sea level 63, Y-64 to Y320) — giving roughly double vanilla's below-sea-level cave volume while keeping the same proportions vanilla has above sea level, so terrain, ore veins, the deepslate transition, clouds, and fluid generation (water/lava placement) are all repositioned consistently with no clipping or lava-flooded caves. Vanilla World is genuinely untouched vanilla generation, for comparison. Every other feature in this mod applies identically to both, since they don't depend on which Overworld generator a save uses.
4. **Energy infrastructure** — Build your own power grid with three tiers (worst-to-best: Iron, Copper, Gold) of **cables** and **stations**. Cables render as a thin connected wire (with a matching thin hitbox) and actively move energy each tick between whatever's adjacent to them — no internal storage of their own — with a flat per-cable loss that gets sharply worse the further a tier's actual throughput falls below its comfortable minimum (Iron has no minimum, so it's always at its flat loss rate; Copper and Gold reward heavier use). A station is a single block with one output face (set toward you on placement, like a furnace) — every other face only accepts energy — and right-clicking it opens a GUI showing the live stored amount, a line graph of that stored amount over time (pick the displayed timeframe with 1h/6h/1d/3d buttons), a live rate of charge/discharge, and a field for typing a precise output rate; storage itself is lossless except for a small continuous leak every second, fastest on Iron and slowest on Gold. An **Energy Monitor** is a thin plate you place by right-clicking any face of a cable — including the top/bottom — like an item frame; it only ever mounts on cables, not stations, since a stored amount has no "direction" for it to show. It shows that exact cable's flow/utilization just by looking at it (red = underutilized for that tier, green = optimal, blue = pushing it as hard as the next tier up is rated for) with a bold arrow that physically rotates to point the way energy is actually flowing — every monitor along the same stretch of cable agrees on one direction, since it's the network's flow, not a per-cable one. Right-clicking it opens the same kind of line graph the station has, of its cable's throughput history. A **Creative Station** (not craftable, creative-inventory only) outputs energy materialized from nothing at whatever rate you set, for testing.
5. **Creeper tech targeting** — Creepers sense and beeline toward blocks tagged `resonantcaves:creeper_attracting_blocks`, pre-populated with this mod's own energy-infrastructure blocks. On arrival, the creeper detonates normally — a standing threat to your power infrastructure that rewards defensive placement.
6. **Creeper packs** — Creepers spawn in packs of 5-10.
7. **Resonant Helmet** — A gold-tier helmet that grants permanent, dimmed Night Vision and a through-walls radar showing every hostile mob within 128 blocks as a faintly breathing orb. Not craftable: zombies have a small chance to spawn wearing one, and always drop it when killed.
8. **Village Core** — Place this block and wire it into your power grid like a station to found a village. Sustained daily power funds growth: real vanilla houses (and farms, pens, and profession buildings — matched to the Core's biome) spring up around it, each with a starter villager, and villagers breed into spare beds over time. The Core never hoards power — it burns whatever's left at the end of each day — so growth has to be earned continuously, not banked up front. Run a deficit for too long and villagers start defecting to pillagers. A **Creative Village Core** (not craftable, creative-inventory only) always has a full tank, for watching the whole growth loop with no wiring at all.

## Requirements

- Minecraft **1.21.1**
- Fabric Loader `>=0.19.3`
- Fabric API
- Java 21
- [TeamReborn Energy](https://github.com/TechReborn/Energy) (bundled with the mod jar)

## Building from source

Building requires a JDK 21 toolchain:

```bash
JAVA_HOME=/path/to/jdk-21 ./gradlew build
```

The output jar is produced at `build/libs/resonantcaves-<version>.jar`.

Other useful tasks:

```bash
./gradlew genSources    # generate decompiled, Yarn-mapped Minecraft sources for reference
./gradlew runClient     # launch a dev client
./gradlew runServer     # launch a dev server
```

## Credits

The cable and station textures are taken from [Tech Reborn](https://github.com/TechReborn/TechReborn) (`copper_cable`/`gold_cable`/`tin_cable`, and the low/medium/high-voltage storage-unit textures), © Tech Reborn contributors, used under the [MIT License](https://github.com/TechReborn/TechReborn/blob/1.21.1/LICENSE.md).

## License

MIT — see [LICENSE](LICENSE).
