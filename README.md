# CrystalTech

CrystalTech is an experimental Forge mod for Minecraft 1.20.1 that compresses a short, perceivable technology spine into three irreversible stages anchored on amethyst resources.

## Getting Started
1. Follow `docs/environment-setup.md` to provision Java 17, Forge MDK, and VS Code integration on macOS.
2. Clone or open this repository in VS Code.
3. Execute `./gradlew genVSCodeRuns` once to generate launch entries.
4. Use the **Minecraft Client** run configuration to start a development instance.

## Stage Overview
- **Stage 0 – Baseline**: vanilla amethyst shard access.
- **Stage 1 – Amethyst Alloy**: right-click with an amethyst shard to advance, consuming the shard and granting the Alloy item.
- **Stage 2 – Crystal Reconstructor**: right-click with the Alloy to progress, consuming it and yielding the Reconstructor.

Player progression is tracked through a Forge capability (`crystal_stage`) that persists through death and exposes a public API/event hook for downstream systems.

## Project Layout
```
src/main/java/com/crystaltech
├─ CrystalTech.java          # mod entry
├─ registry/                 # Deferred registers (items)
├─ capability/               # player stage capability
├─ event/                    # Forge event listeners
└─ core/                     # stage API and custom events
```

Assets live under `src/main/resources/assets/crystaltech`, while documentation resides in `docs/`.

## Commands
- `./gradlew runClient` – launch the Forge development client.
- `./gradlew build` – compile and package the mod jar under `build/libs/`.

## Next Steps
- Expand tests or scripted scenarios covering stage transitions.
- Flesh out future hooks (behavior unlocks, narrative bridges) leveraging `CrystalStageChangedEvent` and `CrystalStageApi`.
