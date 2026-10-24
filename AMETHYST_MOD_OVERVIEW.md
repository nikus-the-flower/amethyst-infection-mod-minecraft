# Amethyst Infection: mod overview

A mod for **Minecraft 1.21.1 on Fabric** (Java 21). Languages: English (base), Russian, Ukrainian.
Mod version: 0.1.0. Author: nikus. The lore (the story of the world) is not written yet: the story page texts are placeholders.

This document describes what the mod already has. Exact numbers were taken from the code at the time of writing and may change with balancing.

---

## 1. The idea

The Amethyst infection spreads across the normal world. In the center of the world stands a tower, a crystal cluster, with the apostle Amitariel. Once he is beaten, the players enter the Amethyst world, where the final boss, the Amethyst Heart, waits in a temple. Beating the Heart stops the infection in the normal world. Along the way there are weapons, artifacts, armor, food, ancient ruins, a purification altar and the trader Ricardo.

Order of play:
1. Start in a village house 250-420 blocks from the center, with a note holding the coordinates of three ruins.
2. Explore the ruins: relics, books, loot. Meet Ricardo.
3. Gather gear, fight the infection and its creatures.
4. Climb the tower in the center of the world, talk to Amitariel and fight him.
5. The rift to the Amethyst world, the temple, the fight with the Heart.
6. Victory, a rift home, the infection is stopped.

---

## 2. The Amethyst infection

- Infected blocks (stone, deepslate, dirt, grass, sand, logs) spread, turning neighboring blocks infected and growing crystals: bud -> crystal -> large crystal, which crystallizes the block it grows from.
- The leaves of infected trees burn away, the trunks crystallize.
- **The infection runs on a single engine with a per-tick budget.** It advances at an even pace and only within 96 blocks of players, so it does not lag however far it grows.
- Speed is set with `/gamerule amethystInfectionSpeed 0-100` (default 100, 0 stops it).
- The infection does not spread in the Amethyst world (it is infected entirely). After the Heart is beaten it stops in the normal world too.
- **Amethyst Blight**: an effect from the bites and attacks of Amethyst creatures. It hurts slowly (never below 3 hearts), slows and weakens attacks.
- The infection is cleansed by: the full Purifier's armor set, the Purifier's Blade, and the Purity Amulet (protects from the blight).
- **Spawns in the normal world:** weak creatures (stalkers and weavers) appear far more often on infected ground, and occasionally on any open-sky surface, day and night.

### Blocks
Infected versions of stone, deepslate, dirt, grass, sand and logs; crystallized stone and deepslate; crystals in three colors (purple, pink, azure) and three sizes; the amethyst crystal block, amethyst bricks and slabs, radiant amethyst (light 15), clear amethyst and its panes. The altar blocks (pedestal and purification altar). The old portal frame and stone remain in the code, but portals were replaced by rifts.

---

## 3. Materials and crafting

- **Amethyst Essence**: made from crystals, the basis of crafting. Crystal block = 4 essence, bricks = 4 blocks -> 4 bricks.
- **Flame Crystal** (essence + 4 blaze rods), **Frost Crystal** (essence + 8 ice): for staffs, the Berserker Core and the Stasis Crystal.
- **Purified Amethyst**: 4 essence + diamond + quartz -> 2. Also obtained on the purification altar.
- **Resonant Shard**: dropped by Amethyst creatures (stalkers and weavers 8%, lurkers 20%, walkers 30%, the colossus always 2-3, Amitariel 3-5), also found in chests. Needed for the good armor, the Crystal Hammer and the shield.
- Radiant amethyst: crystal block + glowstone dust.
- **Heart Core** and the portal core remain as items; the portal was replaced by the rift.

---

## 4. Locations

### 4.1. The Amethyst Tower (0, 0)
A cluster tower: an octagonal crystal trunk inside a geode of hexagonal crystals with sharp tips. The crystals are biggest and widest at the bottom, smaller and more vertical higher up, with the point of the central crystal on top. A stone hill of mother rock surrounds it.
- 5 floors and Amitariel's hall at the top. A spiral staircase of half-slabs along the wall. Glowing rings on the floors, crystals and chandeliers hanging from the ceilings. Entrance from the south.
- Guards: one guard per floor, none flying: stalkers, a lurker, a shard-walker, weavers, a colossus. A chest on every floor (the higher, the better the loot).
- Old towers are rebuilt automatically. Operator commands: `/amethyst tower build`, `/amethyst tower tp`.

### 4.2. The Rift
When Amitariel falls, space tears open in the center of his hall: a tall 3D rift of light that carries you to the Amethyst world (150-500 blocks from the temple). After the Heart is beaten, the same rift opens in the center of the temple arena and leads back to the tower hall.

### 4.3. The Amethyst world
Eternal violet dusk, stars, floating crystal islands. Five biomes: crystal groves, amethyst canyons, quiet meadows, clear amethyst lakes, shard wastelands. Crystal rain falls at times. Creature spawning is reduced. Beds work. The temple stands at the center (0, 0).

### 4.4. The Amethyst Temple
A cathedral under a crystal dome on its own island. Inside: a round arena of radius 30 with columns along the edge, 5 pedestals for pylons and the Heart in the center.

### 4.5. Story sites in the normal world
Chosen by world seed (land, flat ground), 250-1200 blocks from the center. Ruins are built when a player comes within 200 blocks. Commands: `/amethyst sites`, `/amethyst sites tp <house|ruins|crater|castle>`, `/amethyst sites rebuild <...>`.
- **Village house** (the starting point in a new world): stone base, log walls, gabled roof, chimney, kitchen, 4 beds, pantry, a potato garden, 3 chests with basic loot and Country Potatoes. On the lectern is a note with the coordinates of the three ruins (three pages, three languages).
- **Plaza ruins:** old paving, a well, columns, 3 ruined houses, a broken tower. The Purity Shard lies in the tower. Guards: stalkers.
- **Underground ruins:** a crater 10 blocks deep, a spiral staircase along the wall, a ruined bridge over a pond (you can jump into the water), 3 ruined rooms underground, a secret room behind a cracked wall holding the Chaos Flask, an altar with three pedestals in the center. Ricardo's camp is here.
- **Ruined castle:** double crenellated walls, 4 towers, a gate with an arch, a 3-floor keep, a throne hall with the Emperor's Crown in a chest. Guards: a geode colossus and shard-walkers.

---

## 5. The purification altar and relics

- **Relics (3, with 3D models):** the Purity Shard (plaza ruins), the Chaos Flask (underground ruins, hidden), the Emperor's Crown (castle).
- Relics are placed with right click on the three pedestals around the altar (they can be taken back by hand) and are not consumed.
- When all three are in place the altar awakens: light runs from the relics to the altar and a rune ring spins.
- Right-clicking an amethyst item on the altar starts the ritual (4.5 seconds, the item rises and spins, then a flash). It purifies: essence -> Purified Amethyst (the whole stack), amethyst armor -> Purifier's armor, the amethyst blade -> Purifier's Blade, the Spear of Amitariel -> Purified Crystal Spear. Enchantments are kept.

## 6. Ricardo (the trader)

An old wandering scholar (own 3D model: a feathered hat, glasses, a beard, a green cloak, a backpack of scrolls, a staff with an amethyst). He lives by the altar and cannot be hurt.
- **Dialogue window:** a portrait, typewriter text, answers as buttons (who he is, what this place is, the altar, trading).
- **Quest:** assemble the altar and start it. Reward: the Purity Amulet and 2 bowls of Cereal with Milk (once per player).
- **Trading** (a trade window like villagers'): diamond -> 24 essence, 4 gold -> 12 essence. For essence: Cereal with Milk, Country Potatoes, Crystal Bread and Apple, Crystal Arrows, the guide book, Ricardo's diary, simple artifacts (Blink Ring, Vampiric Shard, Gravity Orb, Stasis Crystal).

---

## 7. Creatures

| Creature | Where | Notes |
|---|---|---|
| **Amethyst Stalker** | infected ground, tower floor 1 | crystal wolf, bites and leaps, 34 health |
| **Crystal Weaver** | infected ground, tower | spider, climbs walls, 26 health |
| **Shard-Walker** | tower, castle | tall humanoid, fan of three shards from range, 80 health |
| **Void-Crystal Lurker** | tower, Amethyst world | fast raptor, leaps from range, 46 health |
| **Geode Colossus** | tower (floor 5), castle | huge golem, 240 health, ground shockwave of radius 9, damage reduced by 30% from the original |
| **Celestial Drifter** | Amethyst world | flying, volleys of five shards, 100 health |
| **Ricardo** | underground ruins | trader (see above) |

The damage of all creatures was reduced by about 20% from the first versions and the blight became shorter. No creature needs darkness; each has its own attack poses (animations) and 3D cube models.

---

## 8. Bosses

### 8.1. Amitariel, Apostle of the Amethyst
The hall at the top of the tower. Model: a man in silver-violet plate with white hair, a golden circlet, crystals on the shoulders and back, a cloak and a spear.
- **Conversation:** starts when a player has climbed to his height and he is visible. At the end comes a question: answer "yes" in chat (the player dies) or "no" (the fight).
- **Fight:** 450 health, walks slower than in the first versions. Scales with the number of players.
- **Phase 1:** a dash with a long warning, rune circles and crystal pillars under players, a fan of shards, the **whirlwind** (a circular attack with 3D slashes), the **leaping spear slam** (the spot is marked by a circle, crystals grow at the impact point), **spears of light** (4 spears above his head take aim at players in turn, a thin trajectory line is visible).
- **Phase 2** (below half health): a transformation with a cutscene, a red look, a crystal storm, a teleport behind the player, 6 spears of light.
- Minions: at 80% health two weavers, at 30% one shard-walker.
- **Death:** he falls to his knees, speaks his last words, a rift opens. The **Spear of Amitariel** drops.
- Cutscenes: intro, transformation, death.

### 8.2. The Amethyst Heart
The final boss in the temple. A huge crystal heart (radius 6 and up) with veins, tentacles, armor plates and flying shards. The scale of the fight grows with the number of players. Cutscenes: awakening, phase change, death.

**A 5-stage fight; phases 2-5 each have their own health bar, growing from phase to phase:**

| Stage | Health | What to hit |
|---|---|---|
| 1 | pylons | 5 defensive pylons at the arena edge (they shoot players) |
| 2 "Awakening" | 200 | 4 nodes at the roots |
| 3 "Resonance" | 250 | wounds on the chest and back (the armor has partly fallen off) |
| 4 "Crystallization" | 300 | crystal veins under the belly |
| 5 "Agony" | 400 | the whole body, no defense |

Weak points open only for a few seconds after each Heart attack (double damage); the rest of the body takes little damage. The Heart opens its armor plates in these windows. Old weak points vanish at each stage. Below 8% on the last stage a 30-second countdown to an arena-wide explosion starts. If all players die or leave, the Heart heals and the pylons return.

**Attacks** (picked by weight, the same one is never repeated back to back): an eruption under players, a shard volley, 1-3 shockwaves in a row (a 3D ring, you have to jump), rotating beams along the floor (3D), crystal walls along lines, arena sectors, a pull with an explosion, a blinding flash, minions (1-2 sentinel crystals each, they die in one hit), a pentagram of 5 big crystals around the perimeter, a chase (crystals grow under your feet for 6 seconds), a spiral, a big spiral, a double spiral, growing and falling crystals across the arena (~32-48 per second), the **death beam** (aiming, locking, firing a 3D beam; the higher the stage, the shorter the wind-up and the longer the beam), **random lasers** (stage 5). **Combinations** (one attack made of several "blocks"): spiral + chase, rain + growth, spiral + pentagram, chase + pentagram, "Cataclysm", lasers + death beam, lasers + waves. Stage 5 keeps only the strong attacks and all the combinations.

Victory: the Heart falls, a rift home opens, the infection in the normal world stops. The Heart Core drops.

---

## 9. Weapons

**Swords with abilities (right click):**
- **Amethyst Blade**, **Flame Brand** (a wave of fire), **Frost Edge** (an ice burst), **Storm Blade** (chain lightning), **Void Edge** (a dash with a slash), **Purifier's Blade** (a cleansing burst that burns away the infection).
- **Crescent Blade:** hold right click and release, crescents of light fly out. The longer you hold (up to 3 seconds), the more of them (up to 5), bigger and stronger.

**Staffs:** Flame Staff (hold, flamethrower), Frost Staff (a wave of cold), **Prism Scepter** (hold, a 3D beam that pierces a line).

**Hammers:**
- **Crystal Hammer:** hold right click, you jump and hover, charge for 3 seconds, then slam the ground: enemies are knocked back, crystals grow, you heal, no fall damage.
- **Jaybee Talis's Hammer:** hold up to 5 seconds, a strike of lightning whose radius grows with the charge (up to 12 blocks, up to 40 damage).

**Spears:**
- **Spear of Amitariel** (dropped by him): a short right click gives a whirlwind for 3 seconds; hold and release for a spinning dash with a circular knockback strike at the end.
- **Purified Crystal Spear:** hold right click, spears of light appear above you and fly where you look; ones already launched do not change course. Holding 10 seconds gives a barrage of 18 spears, like a machine gun. Then a cooldown.

**Other:** the Crystal Bow (and Crystal Arrows), the Amethyst Shield (blocks and answers with an explosion), the amethyst pickaxe, axe, shovel and hoe (above iron, 2200 durability).

Weapons have 3D models (not flat icons).

---

## 10. Armor

- **Amethyst:** protection like diamond, crystal crests, shoulder spikes, a glowing back. Full set: Crystal Resonance (more damage, faster hits, higher speed). Each piece: amethyst crystal blocks + a Resonant Shard.
- **Purifier's armor:** heavier, with a halo and wings of light. Each piece gives -15% damage from Amethyst creatures. Full set: Anti-Amethyst Ward (-80% damage from them, immunity to the blight, healing on infected ground, a cleansing aura). Each piece: Purified Amethyst + a Resonant Shard. Can be obtained from the amethyst armor on the purification altar.
- New icons were drawn for both.

---

## 11. Artifacts (carried in the inventory)

- **Aegis Heart:** passive damage resistance; right click for a dense crystal shield.
- **Amethyst Wings:** right click, 25 seconds of free flight.
- **Blink Ring:** speed; right click teleports 14 blocks.
- **Vampiric Shard:** kills restore health.
- **Berserker Core:** 20 seconds of strength, speed and haste, then exhaustion.
- **Stasis Crystal:** freezes enemies within 12 blocks for 8 seconds.
- **Gravity Orb:** pulls in and hits enemies within 16 blocks.
- **Crystal Totem:** saves you from death once.
- **Thunder Shard:** three lightning strikes where you look.
- **Purity Amulet:** immunity to the blight, -25% damage from Amethyst creatures. Given by Ricardo for starting the altar, can be found in tower chests.
- **Aegis of Purity:** charged with Purified Amethyst in the inventory (right click the aegis with essence, 10 pieces for a full charge). While charged: a row of up to 8 white hearts above your health that take damage first and regenerate on their own, and a translucent dome around the player. Each point spends 1% of the charge. Recipe: 7 Purified Amethyst + a shard + a diamond.

---

## 12. Food

- **Purified Crystal Apple:** Regeneration II and Absorption, cures the blight.
- **Purified Crystal Bread:** filling, a little Regeneration, cures the blight.
- **Country Potatoes with Sauce** ("Mmmm, what tasty potatoes!"): Strength and Resistance for 2 minutes.
- **Cereal with Milk:** speed, regeneration, haste, jump boost. Not craftable, sold by Ricardo.

---

## 13. Effects and on-screen effects

- 3D effects instead of particles: beams (aim and shot), wave rings, whirlwinds and a dome (sickles of light), spinning rune warning circles, growing and falling crystals, 3D spears and shards, the rift, altar beams.
- Cutscenes (camera turns, titles, shake): for Amitariel and the Heart.
- Main menu: its own logo, its own panoramas and crystal buttons.
- The white hearts of the Aegis of Purity are drawn above the health bar.

---

## 14. The in-game guide ("Amethyst Compendium")

A guide book with sections: World, Creatures, Bosses, Weapons, Armor, Artifacts. It holds recipes, 3D models of creatures and descriptions of all items and articles: the infection, crystals, materials, the tower, the rift, the Amethyst world, the temple, the ruins, the altar, Ricardo, food, commands, all creatures, bosses, weapons, armor and artifacts. Obtained with book + essence.

---

## 15. Commands

- `/gamerule amethystInfectionSpeed 0-100`: infection speed.
- `/amethyst tower build | tp`: rebuild the tower or teleport to it (operator).
- `/amethyst sites`, `/amethyst sites tp <house|ruins|crater|castle>`, `/amethyst sites rebuild <...>`: story sites (operator).
- `/amethystpanorama`: a panorama snapshot (may produce blank frames with some settings).

---

## 16. Technical notes

- Asset generation: scripts `tools/gen_*.py`, `item3d.py`, `mobs.py` (cube models of creatures), `lang_uk.py`. Generated resources are not edited by hand. Run: `python tools/gen_all.py`, check with `python tools/check_assets.py`.
- Build: `./gradlew build`, output `build/libs/amethystmod-0.1.0.jar`.
- Self-test: `./gradlew runSelftest`, a server run covering the tower, the temple, the Heart fight, the Amitariel fight, the infection, story sites, the altar and Ricardo. Results and screenshots go to `run/selftest/`.
- Not done yet: the lore texts (placeholder pages), compatibility with Epic Fight (needs NeoForge) and Relics, more weapon variety.
