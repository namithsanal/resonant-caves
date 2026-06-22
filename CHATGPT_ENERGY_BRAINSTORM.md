# ChatGPT energy-system brainstorm (historical reference)

This is a condensed summary of `ChatGPT.pdf` (repo root), an old ChatGPT conversation brainstorming a power-grid/energy-infrastructure design for this mod. **None of this was implemented** and it is **not part of the mod build** — see "Relationship to the shipped mod" below for what actually shipped instead.

## Cable tiers with throughput-dependent efficiency

The conversation started from a simpler idea — flat per-cable-segment distance loss (e.g. 1% loss per cable, so 10 cables = ~10% total loss) — and a throughput-bucket alternative (flat loss percentages for 0–100/100–1000/1000+ EU/t bands). Both were superseded by a **voltage-style tier system**: copper/iron/gold cables, each with a minimum "comfortable" throughput and a loss-per-100-blocks rate that gets dramatically worse the further actual throughput falls below that minimum (e.g. a gold cable at 100 EU/t against a 1000 EU/t target loses 90%, but at 5000 EU/t loses almost nothing). The point was to make hard cutoffs feel like *bad performance* ("it's working badly because I'm underutilizing it") rather than a binary "why isn't this working" failure state, so players naturally reach for "do I aggregate into a single gold trunk line, or keep generation local on cheap copper?" without needing an explicit "is my mod novel?" check — a explicit comparison against existing mods (RF/FE "power is just a resource" mods, IndustrialCraft-style "power engineering" mods, Applied Energistics-style logistics mods) concluded the combination of throughput-sensitive efficiency + grid monitoring + infrastructure defense isn't a standard Minecraft mod archetype.

## Rejected: dedicated "substation" routing block

An early idea was a standalone substation block: one input, several outputs, with either percentage-split or priority-ordered routing, used to manage power shortages across districts (factory gets power first, base second, mine gets whatever's left). This was explicitly rejected once the battery redesign (below) made it redundant — the guiding rule of thumb became "every time you think 'maybe I need a substation block,' ask 'could the battery already do that?' If yes, keep the battery." Avoiding a second purpose-built routing block in favor of overloading an existing one was treated as a deliberate simplification.

## Final battery design: lossy tiers that double as flow regulators

Batteries are framed as **power conditioners, not just storage**. Key points:
- Storage itself doesn't decay — loss only happens on **charge/discharge cycling** (e.g. ~95% efficiency each way), so players don't lose power just for forgetting about a battery for a few days; they lose power because they're actively using it as infrastructure.
- Three tiers (copper/iron/gold) differ by capacity, efficiency (80%/90%/97%), and max output (200/1000/5000 EU/t) — deliberately mirroring the cable tiers.
- The interesting mechanic is a **settable target-output rate**: e.g. "Target Output: 2000 EU/t" makes the battery discharge when generation falls below that and charge when it exceeds it. This is the player declaring "this district should receive 2000 EU/t" — a strategic decision without a routing UI, and it's what gives the battery its de-facto substation role.
- The resulting tradeoff: direct transmission is efficient but unstable; battery-buffered transmission is stable but lossy.

## The monitor block

A block that attaches to a cable and reports, without changing the network at all: flow direction, current throughput, average throughput, a utilization status (red = overloaded, green = optimal, blue = underutilized), and a line graph of throughput over the last 3 in-game days. The explicit design constraint was to keep this diagnostic, not a control panel — avoid per-tick data, 20+ statistics, configurable graph ranges, complex routing tables, or forecasting. The monitor's value is that it turns "power line works / doesn't work" into "power line is overloaded / underutilized / spiking only during the day," letting players diagnose problems instead of guessing.

## Final tally and pitch

7 new blocks total: 3 cable tiers + 3 battery tiers + 1 monitor block. Closing one-sentence pitch from the conversation:

> "Build efficient power grids by matching cable tiers to throughput, using batteries to stabilize supply, and monitors to identify bottlenecks."

## Relationship to the shipped mod

None of this was built. The mod's actual Resonant Ore feature (see `CLAUDE.md`, Feature 3) took a deliberately simpler path: a passive, **extract-only** energy source via the TeamReborn Energy API, with no cables, transfer network, storage, or monitoring of any kind — energy availability is just computed on demand from whatever block entity a machine is touching. This matches the project's stated scope rules, which explicitly forbid building "tech-mod infrastructure: machines, power networks, or internal energy storage/buffering" — the cable/battery/monitor system described above is exactly the kind of rabbit hole that scope rule was written to avoid for a ~20-hour first mod.
