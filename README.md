# Resonant Caves

A Fabric mod for Minecraft **1.21.1** that overhauls survival around one core idea: **mining is impossible**. Stone and deepslate are unbreakable, so the game shifts from digging to exploration, hunting, and infrastructure. It is recommended that [Item Obliterator](https://github.com/nvb-uy/Item_Obliterator) is used to remove all other sources of power. I also recommend [Tech Reborn](https://github.com/TechReborn/TechReborn), and [K-Turrets](https://github.com/AlexiyOrlov/k-turrets). An item obliterator config file is provided for Tech Reborn.

## Features

1. **Unbreakable stone & deepslate** — `minecraft:stone` and `minecraft:deepslate` behave like bedrock (no mining animation, no breaking in survival). All other blocks — stone variants, dirt, gravel, and every ore — stay breakable, so resources are only accessible where caves expose them.
2. **Large fleeing herds** — Sheep, cows, pigs, chickens, and horses spawn in much larger groups. Sheep, cows, pigs, and chickens actively flee from players and regroup with nearby herd members, making hunting a real challenge.
3. **Resonant Ore** — A naturally-occurring, bedrock-hard ore (with a deepslate variant) that passively generates a small, randomized amount of RF via the TeamReborn Energy API. Found underground at roughly iron ore's rarity, glowing with a faint light and a coral/salmon-tinted texture.
4. **Elevated world** — The overworld's playable surface generates around **Y 280–320**, leaving the vast space below as cave systems to traverse rather than mine. Caves, vanilla ore placement, and structures are preserved beneath the new surface.
5. **Creeper tech-mod targeting** — Creepers sense and beeline toward blocks tagged `resonantcaves:creeper_attracting_blocks` (pre-populated with ~40 Tech Reborn energy-infrastructure blocks: cables, transformers, storage units, generators, solar panels). On arrival, the creeper detonates normally — a standing threat to your power infrastructure that rewards defensive placement. The tag is extensible to other tech mods via a small datapack.
6. **Creeper packs** — Creepers spawn in packs of 5-10, alongside the large herds of passive animals.
7. **Resonant Crown** — A gold-tier helmet that grants permanent, dimmed Night Vision, a through-walls radar showing every hostile mob within 128 blocks as a faintly breathing orb, and keeps passive animals from fleeing the wearer. Not craftable: zombies have a small chance to spawn wearing one, and always drop it when killed.

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

## License

MIT — see [LICENSE](LICENSE).
