# Amethyst Infection

A Fabric mod for **Minecraft 1.21.1** (Java 21). The amethyst infection spreads across the world; find out where it comes from and stop it.

## What's in the mod

- **Spreading infection**: infected blocks, growing crystals and the Amethyst Blight effect, driven by a lag-safe, budgeted engine (`/gamerule amethystInfectionSpeed`).
- **Story sites**: a starting village house, plaza ruins, underground ruins and a ruined castle, plus the Amethyst Tower in the center of the world.
- **Two bosses**: Amitariel, Apostle of the Amethyst, and the five-stage Amethyst Heart, with cutscenes.
- **The Amethyst world**: a separate dimension with five biomes and a temple, reached through a rift.
- **Creatures**: Amethyst Stalker, Crystal Weaver, Shard-Walker, Void-Crystal Lurker, Geode Colossus, Celestial Drifter, and the trader Ricardo.
- **Gear**: swords, staffs, hammers and spears with abilities, a bow, a shield, two armor sets, artifacts, food.
- **Purification altar**: three relics and a ritual that turns amethyst gear into purified gear.
- **In-game guide**: the Amethyst Compendium.

Languages: English (base), Russian, Ukrainian. The lore texts are not written yet. See [AMETHYST_MOD_OVERVIEW.md](AMETHYST_MOD_OVERVIEW.md) for the full description.

## Made with AI

This project was written almost entirely by AI ([Claude](https://claude.com/claude-code)): the Java code, the generated textures, 3D models, the translations and the documentation. The ideas, design and play-testing are by the human author, nikus. Expect rough edges; balance and visuals are still being tuned.

## Building

```
./gradlew build          # jar in build/libs/
./gradlew runSelftest    # server-side self-test
python tools/gen_all.py  # regenerate assets (generated files are not edited by hand)
```

## License

All rights reserved.
