# CrystalTech Project Plan (MVP → Milestone 3)

## 1. Vision Snapshot
CrystalTech is a rules-first, short technology tree mod for Minecraft Forge 1.20.1. It encodes three perceivable stages anchored on Amethyst progression, acting as a deterministic backbone for narrative, AI, and behavior systems.

## 2. Success Criteria
- Forge mod loads without errors on Minecraft 1.20.1.
- Player capability tracks stages 0→2 with irreversible transitions.
- Stage progression consumes specific items and grants next-stage items.
- External systems can query the player stage through a public API hook.

## 3. Scope Boundaries
**In scope**
- Three custom items (stage markers, placeholder textures).
- Player capability (int) with persistence across sessions.
- Event-driven stage advancement (RightClickItem trigger, no GUI).
- Debug logging for state transitions.
- Basic data assets (lang entries, item models, placeholder textures).

**Out of scope**
- Automation, machines, complex energy systems.
- Cross-mod integrations beyond Forge base APIs.
- Custom sound/visual FX beyond placeholders.

## 4. Functional Specification
### 4.1 Stage Model
```
crystal_stage:
  0 -> Amethyst Baseline (vanilla state)
  1 -> Amethyst Alloy (custom item)
  2 -> Crystal Reconstructor (custom item)
```
- Stored on player capability; saved to player NBT.
- Progression enforced as one-way increments.

### 4.2 Items
- `crystaltech:amethyst_alloy`
- `crystaltech:crystal_reconstructor`
- Uses DeferredRegister (`ForgeRegistries.ITEMS`).
- Placeholder textures reused from vanilla (override via JSON models in `assets/crystaltech/models/item`).

### 4.3 Progression Logic
- Listen to `PlayerInteractEvent.RightClickItem`.
- Validate held item and current `crystal_stage`.
- On success:
  - Consume triggering item(s).
  - Give next stage item (if not yet owned).
  - Update capability value.
  - Emit `CrystalStageChangedEvent` (custom) for future hooks.
- Reject invalid triggers with debug log (and optionally player message).

### 4.4 External Interface
- Capability accessor utility: `CrystalStageAPI.getStage(Player)`.
- Custom Forge event `CrystalStageChangedEvent` (with old/new values).
- Future data-driven hooks placeholder in `core/StageHooks.java`.

## 5. Technical Blueprint
- **Language:** Java 17
- **Mod Loader:** Forge 47.3.0 (1.20.1)
- **Build:** Gradle (MDK wrapper)
- **Entry Mod Class:** `com.crystaltech.CrystalTech`
- **Package Layout:**
  - `registry` – item registration
  - `capability` – capability definition, storage, provider
  - `event` – Forge event subscribers
  - `core` – stage logic, API surface
  - `data` – JSON recipe/placeholders (future)
  - `assets` – lang/model files

## 6. Implementation Roadmap
### Milestone 1 – Minimal Playable (Week 1)
1. Bootstrap MDK project, rename mod id to `crystaltech`.
2. Implement main mod class and logger.
3. Register two custom items with placeholder assets.
4. Implement player capability skeleton with default stage 0.
5. Manual command or temporary creative items to verify capability persistence.
6. Verify `./gradlew runClient` loads mod.

### Milestone 2 – Rule Enforcement (Week 2)
1. Implement RightClickItem handler with stage validation.
2. Add irreversible transition logic and guard clauses.
3. Ensure item consumption and reward logic works in survival.
4. Emit debug logs for start/complete/failure states.
5. Add unit-like tests (if viable) or scripted test checklist.
6. Document testing steps in `docs/testing-checklist.md`.

### Milestone 3 – Integration Hooks (Week 3)
1. Finalize `CrystalStageAPI` utility class.
2. Implement custom event `CrystalStageChangedEvent`.
3. Add data hooks (placeholder JSON) for future narrative integration.
4. Update language files with polished strings.
5. Prepare release notes and onboarding doc for downstream systems.

## 7. Task Breakdown (Milestone 1 Detail)
- Update `settings.gradle` and `build.gradle` mod metadata (mod id, versioning schema `0.1.0-mvp`).
- Remove ExampleMod and sample assets.
- Create package scaffold under `src/main/java/com/crystaltech/`.
- Define `CrystalTech` mod entry with DeferredRegister bootstrap.
- Add `ModItems` registry class registering alloy/reconstructor items.
- Create placeholder model JSON referencing vanilla textures (e.g., `minecraft:item/amethyst_shard`).
- Define capability interfaces: `ICrystalStage`, `CrystalStage`, `CrystalStageProvider`, `CrystalStageStorage`.
- Attach capability via `AttachCapabilitiesEvent<Player>` listener.
- Provide serialization using player persistent NBT.

## 8. Tooling & Automation
- Git branching model: `main` (stable), `feature/*` per milestone component.
- CI (optional future): GitHub Actions running `./gradlew build` on push.
- Debug logging through `LogUtils.getLogger()` with `DEBUG` gate.

## 9. Deliverables per Milestone
- Tagged release `v0.1.0-mvp` (zip with jar, docs update).
- Updated docs: `CHANGELOG.md`, `testing-checklist.md`, `api-reference.md` (stub for milestone 3).

## 10. Risk & Mitigation
- **Forge API shifts**: Pin to specific 47.x version; monitor changelog.
- **Capability serialization bugs**: Implement extensive logging, write NBT unit tests.
- **Resource placeholders**: Accept vanilla textures until art assets ready; document replacements.
- **Team onboarding**: Provide README with quick start; maintain doc updates per milestone.

## 11. Next Steps
1. Follow `docs/environment-setup.md` to prepare local toolchain.
2. Clone/initialize MDK repository and commit clean baseline (`v0.0.1-bootstrap`).
3. Execute Milestone 1 tasks, tracking progress via Git issues or project board.
4. Schedule weekly review to validate milestone deliverables and adjust roadmap.

CrystalTech is now framed with actionable steps, ensuring the MVP can be implemented and extended systematically.
