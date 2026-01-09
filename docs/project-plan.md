# CrystalTech Project Plan (技线升级版)

## 1. Vision Snapshot
CrystalTech 是一个以紫水晶为中心的技术线 Forge 模组，通过「工作台 → 机器 → 能量 → 物流」构建可感知的阶段推进。整个体验围绕 Amethyst 资源展开，为叙事、AI 与行为系统提供确定性的推进依赖。

## 2. Success Criteria
- 模组在 Minecraft Forge 1.20.1 中无报错加载。
- 玩家 Stage 能力值记录阶段 0 → 2 的不可逆推进，并预留阶段 3。
- 阶段推进仅来源于配置配方或机器产出（不允许右键捷径）。
- 机器、能量、物流系统提供基础功能并可被 Stage 校验。
- 对外公开 `CrystalStageApi` 查询与事件监听接口。

## 3. Scope Boundaries
**In scope**
- 三个核心物品：`amethyst_powder`、`quartz_powder`、`amethyst_alloy_ingot`。
- 玩家 Stage 能力、NBT 持久化与 API 查询接口。
- 数据驱动的阶段推进：工作台配方、高炉/机器产出。
- 紫晶熔炉（功能方块 + BlockEntity + FE 能量槽）。
- 紫晶砖块（多方块结构基础组件）。
- 紫晶光缆（低距离 FE 物流）。
- 中英文语言文件、占位模型/配方 JSON。
- 调试日志与 GameTest 验证流程。

**Out of scope**
- 复杂自动化流水线与跨模组联动（预留 Stage 3 扩展）。
- 自定义音效/特效（暂用占位资源）。
- 完整美术 CTM 资源（仅预留结构与命名）。

## 4. Functional Specification
### 4.1 Stage Model
```
crystal_stage:
  0 -> 紫水晶基线 (原版采集)
  1 -> 紫水晶材料化 (工作台配方)
  2 -> 紫晶工业化 (高炉/机器)
  3 -> 紫晶系统化 (预留，多方块+物流)
```
- 存储于玩家 Stage Capability，并写入 PlayerPersisted NBT。
- 通过配方/机器唯一触发，禁止右键跳级。

### 4.2 Items & Blocks
**Items**
- `crystaltech:amethyst_powder`
- `crystaltech:quartz_powder`
- `crystaltech:amethyst_alloy_ingot`
- `crystaltech:amethyst_alloy_pickaxe`（Stage 2 解锁）
- `crystaltech:amethyst_alloy_sword`（可选）

**Blocks**
- `crystaltech:amethyst_furnace`
- `crystaltech:amethyst_bricks`
- `crystaltech:amethyst_flux_cable`

均使用 `DeferredRegister` 注册，提供模型、lang、占位纹理。

### 4.3 Progression Logic
- Stage 0 → 1：监听 `PlayerEvent.ItemCraftedEvent`，当玩家首次合成 `amethyst_powder` 或 `quartz_powder` 时推进。
- Stage 1 → 2：由 `AmethystFurnaceBlockEntity` 在输出槽首次生成 `amethyst_alloy_ingot` 时推进。
- Stage 2 → 3：预留，通过多方块结构与能量体系完成后触发。
- 推进时调用 `CrystalStageApi.advance(ServerPlayer, CrystalStage)`，广播自定义事件 `CrystalStageChangedEvent`。

### 4.4 External Interface
- 能力访问工具：`CrystalStageApi.getStage(Player)`、`CrystalStageApi.isAtLeast(Player, stage)`。
- 自定义事件 `CrystalStageChangedEvent`，提供旧/新阶段、触发类型（配方/机器）。
- 为数据驱动的 Stage 条件提供 `stage_unlocked` 配方条件与 BlockEntity 标志查询。

### 4.5 Machines & Energy
- `AmethystFurnaceBlockEntity`：提供输入、辅助、输出、能量槽；消耗 FE；支持多方块检查。
- `AmethystFluxCableBlockEntity`：短距离 FE 分配；发光渲染；最大连接 8 格。
- 能量系统采用 Forge Energy（不主动发电，Stage 3 引入）。

## 5. Technical Blueprint
- **Language:** Java 17
- **Mod Loader:** Forge 47.3.0 (1.20.1)
- **Build:** Gradle (MDK wrapper)
- **Entry Mod Class:** `com.crystaltech.CrystalTech`
- **Package Layout:**
  - `registry` – Items、Blocks、BlockEntities、Menus 注册
  - `capability` – Stage 定义、存储、提供者
  - `event` – 配方监听、能力同步、GameTest 钩子
  - `core` – Stage API、Stage 条件与事件
  - `content` – 功能方块、机器逻辑、物流实现
  - `data` – 配方占位、标签
  - `assets` – 模型、纹理、lang

## 6. Implementation Roadmap
### Milestone 1 – Stage 基础 (Week 1)
1. 清理示例代码，确认 mod id `crystaltech`。
2. 实现主类、日志、`DeferredRegister` 初始化。
3. 注册粉末与合金核心物品，提供模型、lang 占位。
4. 搭建 Stage Capability，保证持久化与 API 查询。
5. 在工作台配方监听中完成 Stage 0 → 1 推进逻辑。
6. 通过 `./gradlew runClient` 验证加载与基础推进。

### Milestone 2 – 机器与能量 (Week 2)
1. 实现 `AmethystFurnace` 方块与 BlockEntity，含 GUI、FE 能量槽。
2. 编写高炉/机器配方占位，绑定 Stage 1 → 2 触发。
3. 添加紫晶砖块与多方块检测逻辑占位。
4. 完成紫晶合金工具 Tier 与配方条件。
5. 建立 `CrystalStageChangedEvent`、调试日志与 GameTest。

### Milestone 3 – 物流与 Stage 3 预留 (Week 3)
1. 实现 `AmethystFluxCable` 能量传输与渲染占位。
2. 完善多方块结构（紫晶熔炉）检测、状态同步与提示。
3. 设计 Stage 2 → 3 的占位触发逻辑与 API 预留。
4. 扩充配方条件、lang 与文档更新（`crystaltech-implementation`）。
5. 准备发布说明、测试清单与后续扩展路线。

## 7. Task Breakdown (Milestone 1 Detail)
- 更新 `settings.gradle`、`build.gradle` 元数据（`mod_version = 0.2.0-techline`）。
- 移除示例模组代码与资源。
- 搭建包结构：`registry`、`capability`、`core`、`event`。
- 定义 `CrystalTech` 入口类并注册 Items、Blocks 占位。
- 实现 `ModItems`：粉末、合金锭、工具（暂仅注册）。
- 创建占位模型 JSON 指向临时纹理。
- 定义 Stage Capability：接口、实现、Provider、Storage。
- 在 `AttachCapabilitiesEvent<Player>` 中附加能力，处理同步与持久化。
- 编写 `PlayerEvent.ItemCraftedEvent` 监听，完成 Stage 0 → 1 逻辑与日志。

## 8. Tooling & Automation
- Git 分支：`main`（稳定）、`feature/<milestone>`。
- 计划添加 GitHub Actions，执行 `./gradlew build` 与 `./gradlew prepareRunClientCompile`。
- `LogUtils.getLogger()` 作为统一日志入口，阶段推进打印 DEBUG。
- GameTest 模块覆盖 Stage 推进、BlockEntity Tick、能量传输。

## 9. Deliverables per Milestone
- 里程碑标签：
  - `v0.2.0-alpha`（Stage 基础）
  - `v0.3.0-beta`（机器与能量）
  - `v0.4.0-preview`（物流与 Stage 3 预留）
- 文档更新：`CHANGELOG.md`、`testing-checklist.md`、`crystaltech-implementation.md`、`api-reference.md`。
- 数据资源：配方占位、lang、模型、CTM 目录结构。

## 10. Risk & Mitigation
- **Forge API 变动**：固定 47.3.0，跟踪 patch notes。
- **能力序列化问题**：单元测试 + GameTest 验证 + DEBUG 日志。
- **机器/能量复杂度**：先实现占位逻辑，逐步替换为正式数值。
- **资源缺口**：占位纹理与 CTM 目录预留，后续批量替换。
- **性能风险**：多方块与能量传输需做范围限制与 Profile。
## 11. Next Steps
1. 根据 `docs/environment-setup.md` 搭建环境。
2. 创建 `feature/milestone-1-stage` 分支，执行 Milestone 1 任务。
3. 同步 `crystaltech-implementation.md`，按模块拆分 Issue。
4. 每周评审一次阶段推进与测试状态，滚动更新路线图。

CrystalTech 现已对接紫水晶技术线落地文档，可按阶段逐步实现并验证。
