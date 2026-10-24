# Plan: reworking the amethyst infection (core crystals)

Status (2026-10-09): **steps 1-7 are all done**, the self-test passes (SUCCESS). Step 3 (the biome) caused no lag: 0.12 ms per tick on average, 2.2 ms in the worst tick, one chunk's biome change up to 1.5 ms once a second. The original plan follows; accepted decisions are marked ✔.

Code: `CoreManager` (cores, geodes, phases, commands), `InfectionManager` (painting the ground), `world/CoreState` (data), `entity/InfectionCoreEntity`, `item/CrystalGeodeItem`, `item/ModTags`. Not verified in the client: the core model, the foliage and geode textures, the look of the biome.

## 1. The idea in two sentences

The infection does not spread as a "blob" but from **cores**: crystal entities scattered across the world. A core grows over time (3 phases), infects the ground around it, summons monsters in later phases, attacks the player, is protected by a barrier and scatters **geodes** that grow into new cores. Killing a core in phase 1 is easy; in phase 3 only Purified Amethyst works.

## 2. Accepted decisions

- ✔ Cores appear **on their own during world generation**, scattered across the map.
- ✔ A zone of radius 50 blocks (diameter 100) around **0,0** (the tower) is infected from the very start: turf, dirt, trees. It is static, with no cores inside. The tower and its buildings are left alone.
- ✔ Phase 1 infects **only the surface**: turf, dirt, sand, trees (no stone or deepslate, no dungeons). Leaves become crystalline instead of disappearing.
- ✔ Phases grow **with time** (by the core's age), without jumps. There are **commands** to upgrade or create a core and a geode of a given level.
- ✔ Anti-amethyst = **Purified Amethyst**. We create the tag `amethystmod:purified_amethyst` (items) and check weapons that pierce the barrier against it. The tag is built in `tools/gen_content.py` (data is not edited by hand).
- ✔ The ground **stays infected** after a core dies: the core only stops feeding the growth.
- ✔ Cleansing of the ground starts **only after the Amethyst Heart is beaten** and is very slow. Purifying armor keeps cleansing around itself.
- ✔ After the Heart is beaten, cores **disappear gradually** (each has its own timer of several days). No new cores or geodes appear.
- ✔ The tower stays the portal/boss. It is not given the "mother core" role. Beating Amitariel does not affect the cores.

## 3. The Purified Amethyst tag

`amethystmod:purified_amethyst` (item). Contents:
- everything the altar produces (`AltarBlockEntity.purified(...)`): Purified Amethyst, purified weapons and armor, the purified spear;
- `purity_shard` and other purified relics;
- purified armor is checked by the `PURIFIED` material, and purified projectiles (`PhantomSpearEntity` with the `PURE` flag) by the flag.

New purified weapons added later just need to be added to the tag.

## 4. How to avoid lag and still infect the whole world

- A core = **a record in the world data** (position, birth moment, phase). Thousands of records can be kept, they are nearly free.
- The core entity is created when chunks nearby are loaded. Far from players (beyond ~96 blocks) the core "sleeps" but counts as alive.
- **Catch-up infection**: the size of the infected area depends on the core's age. When a player approaches, the area is already infected as if the core had been growing all along. Work is done in chunk-sized portions to avoid hitches.
- Growth is limited by **density**, not total count: a minimum distance between cores (~64 blocks) and an overall record cap (~3000). Geodes land only on free spots, so growth saturates and is not explosive.
- The geode cooldown is long and random. New cores always start at phase 1.
- `InfectionManager` stays budgeted (1 ms per tick). The border is fed only by cores, not by random infected blocks.

## 5. Core phases

Default thresholds (configurable with a game rule): phase 2 after roughly 10 game days, phase 3 after roughly 25.

| | Phase 1 | Phase 2 | Phase 3 |
|---|---|---|---|
| Look | a static crystal, like an ordinary one | taller (about 2 blocks) | bigger, stronger |
| Killing | easy | harder | only with Purified Amethyst (barrier) |
| Infection | surface only, slow | faster, denser | fastest |
| Mobs | none | weak Amethyst mobs nearby (per-core limit) | more dangerous mobs |
| Landscape | small crystals | crystal spikes and the **"crystal wastes" biome** | the same, wider |
| Attack | none | none | the core attacks the player (beams/crystals) |
| Geodes | no | yes | yes |

## 6. Cores at world generation

- The world is divided into cells (~400x400); each has a chance (by seed) of getting a core.
- Not closer than 150-200 blocks to the tower.
- **Danger gradient**: the farther from 0,0, the older the core at generation (phase 1 closer in, phases 2-3 farther out).
- The world keeps "aging": cores grow and seed new ones.

## 7. Commands and the geode

`/amethyst core ...`
- `spawn <phase> [position]`: create a core.
- `upgrade [all|radius]`: raise the phase of the nearest one, of all in a radius, of all.
- `set <phase>`: set the phase of the nearest one.
- `list`: cores nearby (position, phase, age).
- `kill [all|radius]`: remove.
- `geode <phase>`: give a geode.

**The geode** is an item with a level (a component). Thrown or planted, it produces a core of that level. One geode per phase in the creative tab.

## 8. After the Heart is beaten

- New cores and geodes stop appearing.
- After a random number of days each core dims, cracks and disappears.
- Very slow cleansing of the ground (a low per-tick budget, built on the existing cleansing).

## 9. Work steps

Each step ends with the self-test (`./gradlew runSelftest`) and your play-testing.

1. **Foundation**: core storage, restricting the infection to the surface, the phase 1 core entity, seed-based core generation, the zone around 0,0, the Purified Amethyst tag.
2. **Growth**: phase and radius by age, catch-up infection, density limit, the `core` commands, the speed game rule.
3. **Phase 2**: the tall core, weak mobs with a limit, crystal spikes, the "crystal wastes" biome (biome change in portions).
4. **Phase 3**: the core's attack, the barrier pierced only by Purified Amethyst, more dangerous mobs.
5. **Geodes**: the item, the `geode` command, scattering from phase 2-3 cores, cooldowns and planting rules.
6. **Beating the Heart**: gradual disappearance of cores, geodes stop, slow cleansing.
7. **Feedback**: sounds and particles at the border, hints for the player, advancements.

## 10. Risks

- The biome change (step 3) is the heaviest spot for performance. We start with a small radius and check it with the self-test.
- Catch-up infection on approach has to be queued per chunk.
- The visuals of the new models and the biome are checked by the player with screenshots (the client does not run in this environment).
- Balance of the phase thresholds and speed is tuned from your tests.
