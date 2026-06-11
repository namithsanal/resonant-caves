# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Current state

The project is scaffolded as a Fabric mod targeting **Minecraft 1.21.1** (pinned versions under "Architecture & tech decisions"). **Feature 1 (unbreakable stone & deepslate) is implemented and verified** — bedrock-like (no mining animation) via a mixin plus a server-side veto. **Feature 2 (large fleeing herds) is implemented and verified** — biome spawn group sizes raised via Fabric's `BiomeModifications` API and a `FleeEntityGoal` injected into sheep/cow/pig/chicken via mixin; horses get larger herds but stay tame. **Feature 3 (Resonant Ore) is implemented and verified** — two bedrock-hard ore blocks with a block entity that rolls a 0–15 RF/t generation rate and exposes it via the TeamReborn Energy API, generated underground at roughly iron ore's rarity via `BiomeModifications.addFeature`. `./gradlew build` is green and a headless server smoke test loads the mod + mixins + worldgen cleanly. Feature 4 is not yet built. The original design conversation is preserved in `ChatGPT.pdf`. Compiling requires a JDK 21 (see "Building & running").

## What this project is

A **single Fabric mod** for Minecraft Java Edition implementing a "Surface Survival" overhaul. The core fantasy: **mining is impossible**, so the game becomes about exploration, hunting, and infrastructure instead of digging.

- The playable surface generates near build height (~Y 270–290), leaving huge cave systems below as *traversal* space, not mineable rock.
- Stone and deepslate are **unbreakable** (like bedrock); everything else — including stone variants (granite, diorite, andesite, tuff), dirt, gravel, and all ores — stays breakable, so resources remain accessible only where exposed in cave walls.
- Passive animals spawn in large, skittish herds that flee players — making hunting a real challenge and animals a critical resource.
- A naturally-occurring unbreakable ore ("Resonant Ore") passively emits a small amount of RF, so energy comes from finding and claiming cave nodes rather than tech-mod machines.

## Hard scope discipline (read before adding anything)

This is a ~20-hour, AI-assisted, *first* mod. The design explicitly rejects features that become rabbit holes. **Do NOT build:**

- A custom chunk generator from scratch — modify vanilla `noise_settings` instead.
- Custom pathfinding or terrain-safety navigation — vanilla pathfinding is acceptable for v1.
- A rewritten mob AI system or true flocking/boids — fake herd behavior with large spawn groups + a strong flee radius.
- Migration/sleeping/predator-prey systems.
- Tech-mod infrastructure: machines, power networks, or internal energy storage/buffering.

**Do:** keep implementation simple, lean on vanilla systems with small tweaks, and use existing APIs.

## The four features (MVP)

Build in this order; **worldgen is last** because debugging it can consume the entire budget.

1. **Unbreakable stone & deepslate** *(Easy — built ✓)* — `minecraft:stone` and `minecraft:deepslate` only; all other blocks (stone variants granite/diorite/andesite/tuff, dirt, gravel, ores) stay breakable. Implemented in two layers sharing `UnbreakableBlocks.isUnbreakable`: (a) `AbstractBlockStateMixin` forces `calcBlockBreakingDelta` to `0` so survival players get the **bedrock feel** — no cracking animation, no progress; (b) a `PlayerBlockBreakEvents.BEFORE` handler is an authoritative server-side veto. Creative players are exempt (they break via a separate instant path). Verified compiling + a headless server smoke test.
2. **Large fleeing herds** *(Easy — built ✓)* — `HerdSpawning` uses Fabric's `BiomeModifications` API to rewrite the min/max group size of every existing overworld spawn entry for sheep (15–25), cows (10–20), pigs (8–16), chickens (20–40), and horses (6–12), preserving each biome's original spawn group/weight so only herd size changes. `FleeingAnimalsMixin` is a single multi-target mixin (`@Mixin({SheepEntity, CowEntity, PigEntity, ChickenEntity})` extending `AnimalEntity`) that injects two goals at the tail of `initGoals`: a `FleeEntityGoal<PlayerEntity>` (32-block detection, slow/fast speeds 1.5/2.0, priority 1), and `StayNearHerdGoal` (priority 2) — a `FollowParentGoal`-style "walk back toward your nearest same-type neighbor once >16 blocks away" cue that keeps scattered herds re-cohering without true flocking. **Horses are exempt from both — they spawn in large herds but do not flee or regroup.** Goals are injected via mixin, not entity replacement, so breeding/drops/pathfinding/animations are untouched. Verified compiling + a headless server smoke test.
3. **Resonant Ore** *(Medium — built ✓)* — `ModBlocks` registers `resonant_ore` (stone variant) and `deepslate_resonant_ore`, both bedrock-hard via `AbstractBlock.Settings.strength(-1.0F, 3600000.0F)` — naturally unmineable in any game mode, no mixin needed (`UnbreakableBlocks.isUnbreakable` lists them too, for a single source of truth). Only `resonant_ore` gets a `BlockItem`/creative-tab entry, since neither block is ever obtainable in survival. `ResonantOreBlockEntity` rolls a single `generationRate` (0–15 RF/t) from a triangular distribution peaking at 4, seeded deterministically from the block's position (and persisted to NBT). `ResonantEnergyStorage` is a stateless, extract-only `team.reborn.energy.api.EnergyStorage` (refuses insertion; `getAmount`/`getCapacity`/`extract` all read `generationRate` directly) registered via `EnergyStorage.SIDED.registerForBlockEntity`. Worldgen is a `minecraft:ore` configured feature + placed feature (`data/surfacesurvival/worldgen/...`, count 48, size 6, relative `above_bottom`/`below_top` height range so it survives Feature 4's height changes) registered with `BiomeModifications.addFeature` at `GenerationStep.Feature.UNDERGROUND_ORES` — roughly iron ore's rarity, may be buried (`discard_chance_on_air_exposure: 0.0`). Verified compiling + a headless server smoke test.
4. **Elevated world** *(Medium — build last)* — Shift terrain upward (surface ~Y 270–290) by modifying vanilla noise settings; keep the **vanilla biome source** so caves, aquifers, ravines, structures (villages, strongholds, mansions, ruined portals), trees, and ores are all preserved for free. Expect flatter terrain / reduced mountains since the build cap is 320. Strongholds/ancient cities may clip into unbreakable rock — rely on caves and exposed generation to keep them reachable.

## Architecture & tech decisions

- **Loader:** Fabric (Fabric Loom + Fabric API). Start from the [Fabric Example Mod](https://github.com/FabricMC/fabric-example-mod) template.
- **Language/tooling:** Java, IntelliJ IDEA.
- **Minecraft version (pinned): 1.21.1**, with **Yarn mappings** (`yarn 1.21.1+build.3`), **Java 21**, Fabric Loader `0.19.3`, Fabric API `0.116.12+1.21.1`, Loom `1.16-SNAPSHOT`, Gradle `9.4.1`. We deliberately chose the established 1.21.1 over the newest Minecraft (26.x): version mismatch — not coding — is the biggest risk, and 1.21.1 has the deepest tutorial/example base, the most mature library support (including TeamReborn Energy), and well-settled tooling. We use **Yarn** rather than Mojang mappings (the current example-mod template defaults to Mojang) because the 1.21.1 tutorial/example ecosystem is overwhelmingly Yarn-based. **Match all docs/examples to 1.21.1 + Yarn.**
- **Loom plugin gotcha:** Loom 1.16 splits the Gradle plugin. The plain `net.fabricmc.fabric-loom` plugin does **not** expose the `mappings`/`modImplementation` DSL (it has a mappings-less workflow). For the classic Yarn setup (`mappings "net.fabricmc:yarn:…:v2"` + `modImplementation`), use **`net.fabricmc.fabric-loom-remap`** — which is what `build.gradle` declares.
- **Worldgen lives as datapack-style JSON bundled inside the mod jar** (noise settings, world preset, configured/placed ore features) — not a separate datapack and not a Java chunk generator. This keeps the deliverable a single mod file ("install mod and play") while minimizing Java code.
- **AI goal injection** uses SpongePowered Mixin.
- **Energy** uses the **TeamReborn Energy API** (the mature Fabric-native equivalent of Forge RF). The ore only *reports available extraction and refuses insertion* — energy availability is computed on demand, so no save data or sync is needed.

## Building & running

The project is scaffolded (Loom + Fabric API) and builds successfully. Standard workflow:

```bash
./gradlew genSources    # generate decompiled, Yarn-mapped Minecraft sources for reference
./gradlew runClient     # launch a dev client to test in-game
./gradlew runServer     # launch a dev server
./gradlew build         # compile + produce build/libs/surfacesurvival-<ver>.jar
```

**Gradle must run on a JDK 21** (Loom compiles and runs Minecraft 1.21.1 against Java 21; there is no Gradle toolchain configured, so Gradle uses the JVM that launches it). Fedora 44 ships no JDK 21 package (only 25/26), so this repo uses a user-local **Temurin JDK 21** at `/home/namith/jdks/jdk-21.0.11+10`. Invoke Gradle with that JDK:

```bash
JAVA_HOME=/home/namith/jdks/jdk-21.0.11+10 ./gradlew build
```

(Or set IntelliJ's Gradle JVM to that JDK.) There is no test-suite convention yet; add one when tests are introduced.

## Development workflow

Build **one feature at a time**, fully closing the loop each time: verify it compiles → launch the game → test the behavior in-world → commit → move to the next feature. Get a working mod (unbreakable stone) *before* touching worldgen.