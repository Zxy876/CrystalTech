# CityPhone 现状与角色说明

## 1. 项目内的定位
- CityPhone 被设计成 Ideal City 体验中的 "副设备"：玩家主动打开它来理解世界的状态，而不是通过它执行关键操作。
- 在文档 `docs/IDEAL_CITY_CRYSTALTECH_PROTOCOL.md` 与 `docs/crystaltech-manifestation-plan.md` 中反复强调：Forge（主世界）负责行动与显化，CityPhone 负责解释、记录与归档。它不会抢占主窗口，也不会自动弹出。
- 在 `DRIFT_SCIENCELINE/README.md` 中，CityPhone 被描述为档案馆界面和气氛播发的桥梁：后端裁决结果写入 CityPhone，再由插件/Mod 呈现给玩家。
- 这种定位确保玩家即便不打开 CityPhone 也能继续游戏，但打开 CityPhone 时能看到城市的阶段进展、风险提示与社会反馈。

## 2. 系统边界与数据流
```
Forge / CrystalTech Mod              协议目录 (protocol)                 Drift 后端 / City 系统
 ┌──────────────────┐           ┌───────────────────────────┐          ┌──────────────────────────┐
 │ 玩家操作、阶段推进 │  写入/读取  │ city-intents/ …                          │  │ REST 裁决、故事态保存 │
 │ 社会事件钩子       ├──────────▶│ cityphone/social-feed/*.json(l)	 │◀────────┤ city-intents pending │
 │ CrystalTech 输出   │          │ cityphone/technology-status.json │          │ CityPhone state API  │
 └──────────────────┘           └───────────────────────────┘          └──────────────────────────┘
```
- `ProtocolFileLayout` (`src/main/java/com/crystaltech/protocol/ProtocolFileLayout.java`) 负责解析 `CRYSTALTECH_PROTOCOL_ROOT` 或游戏存档路径，统一确定 `cityphone/` 与 `city-intents/` 等共享目录。
- CrystalTech Mod 侧负责写入：
  - `SocialFeedWriter` 将阶段晋升等事件写入 `cityphone/social-feed` 并维护 `events.jsonl` 与 `trust_index.json`。
  - `TechnologyStatusWriter` 维护 `cityphone/technology-status.json`，记录阶段、能源、风险与近期事件。
- Drift/后端侧负责消费与整理：
  - `backend/app/core/ideal_city/social_feedback.py` 读取 `events.jsonl`、`metrics.json` 等文件形成 UI 快照。
  - `backend/app/core/ideal_city/technology_status.py` 将 `technology-status.json` 解析成结构化对象，供 `/ideal-city/cityphone/state` 等接口使用。
  - `backend/scripts/check_protocol_end_to_end.py` 对协议目录进行验收，确保 CityPhone 所需数据齐备。

## 3. CrystalTech (Forge) 端实现
- **玩家入口**：`/cityphone` 命令（`src/main/java/com/crystaltech/event/CityPhoneCommands.java`）会采集玩家的显化意图 (`PlayerIntentCapability`) 并通过 `CrystalTechNetwork` 推送 `CityPhoneDataMessage` 给客户端。
- **数据模型**：`CityPhoneDataMessage.CityPhoneSnapshot` 封装当前阶段、可执行阶段上限、活跃意图 ID、约束、上下文提示、过期时间，以及最近一次完成的意图信息。
- **客户端界面**：`CityPhoneClient` 与 `CityPhoneScreen` 在客户端渲染 CityPhone 视图，展示上述快照以及格式化后的时间戳。
- **协议输出**：
  - `SocialFeedWriter` 以原子写入方式创建事件文件，并维护信任指数增量（默认 0.05 随阶段推进变化）。
  - `TechnologyStatusWriter` 在多线程环境下通过加锁写入，保证 `technology-status.json` 的一致性；提供 stage/energy/risks 及 recent_events。

## 4. Drift/后端侧职责
- 虽然当前仓库未直接包含 `/ideal-city/cityphone` API 的实现，但 `DRIFT_SCIENCELINE/README.md` 明确后端通过 `POST /ideal-city/cityphone/action` 与 `GET /ideal-city/cityphone/state/{player}` 对外提供 CityPhone 提交与状态接口。
- 在测试 (`CrystalTech/backend/test_ideal_city_pipeline.py`) 中，`IdealCityPipeline.cityphone_state(...)` 会读取协议目录生成玩家 CityPhone 状态，并断言：
  - 阶段/风险信息来自 `technology-status.json`。
  - 社会反馈通过 `cityphone/social-feed` 补充。
  - 研究提示 (`research_hint`) 会同步到 CityPhone 状态，用于 UI 展示阻塞原因。
- `simulate_protocol_worker.py` 提供工具脚本，模拟写入 CityPhone artefact，方便本地联调。

## 5. 运维与校验要点
- **目录配置**：环境变量 `CRYSTALTECH_PROTOCOL_ROOT` 控制 Mod 端写入路径，需与后端读取路径保持一致；默认位于 `run/protocol_*`。
- **文件一致性**：
  - `TechnologyStatusWriter` 与后端 `TechnologyStatusRepository` 对字段命名做了兼容（例如 `risks` vs `risk_alerts`、`events` vs `recent_events`），确保旧格式仍可解析。
  - `SocialFeedWriter` 写入的 `trust_index.json` 在后端被 `SocialFeedbackRepository` 读取并转换为 `trust_index` 与 `stress_index` 指标。
- **测试矩阵**：
  - `backend/test_social_feedback.py` 验证社会事件与指标解析。
  - `backend/scripts/check_protocol_end_to_end.py` 可作为 CI/运维脚本检查 CityPhone Artefact 是否完整。
  - Forge Mod 侧可通过运行 `/cityphone` 命令快速验证 UI 是否能输出 `CityPhoneSnapshot`。

## 6. 当前作用总结
- **玩家体验层**：CityPhone 是理想城市线的档案终端，提供阶段、风险、能源、社会反馈、研究提示等信息，帮助玩家理解当前显化上下文。
- **系统协调层**：它串联 Forge/CrystalTech Mod 与 Drift 后端的协议目录，是交互闭环的观察面板：后端裁决 -> 写入协议目录 -> Mod 写 UI 快照 -> 玩家查看 -> 玩家操作再回到后端。
- **扩展性**：`TechnologyStatusWriter`、`SocialFeedWriter` 与 `ProtocolFileLayout` 均考虑了 future-proof（字段兼容、环境变量、事件上限），支持后续增加更多 CityPhone 终端或新增指标。

通过以上组件，CityPhone 在整个项目中承担“解释世界”的职责，同时保持与核心建造/显化流程解耦，确保玩家体验与系统维护都能够围绕清晰的协议与 artefact 管理展开。
