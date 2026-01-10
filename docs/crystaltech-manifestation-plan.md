# CrystalTech × Ideal City 协议落地（Forge 侧实现指引）

> 本文为 CrystalTech（Forge 模组）内部落地文档，约束如何接入 Ideal City Plugin 提供的 Manifestation Intent。我们只负责显化侧实现，不越权执行 Plugin 职责。

## 0. 协议立场
- **唯一入口**：仅接受来自 Plugin 的 `ManifestationIntent` 对象。
- **唯一出口**：通过 Forge 既有的阶段、配方、机器等机制反馈世界变化。
- **绝不为 Plugin 判定世界观**：拒绝任何绕过 Intent 的推进行为。

## 1. 数据桥设计
### 1.1 Intent 载体
```json
{
  "intent_id": "EXAMPLE_STAGE_1",
  "intent_kind": "CRYSTAL_TECH_STAGE_UNLOCK",
  "schema_version": "0.1.0",
  "scenario_id": "default",
  "allowed_stage": 1,
  "confidence_level": "research_validated",
  "constraints": ["no_stage_skip"],
  "notes": ["示例：城市系统允许紫水晶基础材料化探索"],
  "issued_at": "2026-01-10T12:00:00Z",
  "expires_at": "2026-01-11T12:00:00Z",
  "metadata": {"source_spec_id": "EXAMPLE_SPEC"},
  "signature": "ideal-city::example"
}
```

### 1.2 Forge 接口封装
- `com.crystaltech.protocol.ManifestationIntent`（POJO）
  - 当前存储字段：`intentId`, `scenarioId`, `scenarioVersion`, `allowedStage`, `confidenceLevel`, `constraints`, `contextNotes`, `expiresAt`
  - 只读属性；从 Plugin 序列化内容反序列化
  - 额外键（如 `intent_kind`、`schema_version`、`metadata`、`signature`）由 Codec 忽略，不参与计算但不得破坏解析
- `ManifestationIntentCodec`
  - 负责 JSON ⇄ POJO 转换
  - 拒绝缺失关键字段或非法类型
  - 可选字段：
    - `scenarioVersion`：字符串，记录场景版本（例如 `2026.01`）；Forge 原样存储供审计
    - `expiresAt`：UTC ISO 8601，缺失表示长期有效
    - `contextNotes`：字符串数组，用于解释意图背景
    - 其他字段（如 `metadata`、`signature`）会被忽略但允许存在

### 1.3 传输方式（唯一方案：目录式收件箱）
- 双方约定唯一媒介 `city-intents/` 目录，结构如下：
  ```
  city-intents/
  ├─ pending/        # Plugin 原子写入新 Intent
  ├─ processing/     # Forge 拉取后移动至此
  ├─ processed/      # Forge 完成处理归档
  └─ failed/         # Forge 校验失败或过期归档
  ```
- Forge 实现 `FileManifestationIntentInbox`：
  - `poll()`：扫描 `pending/`，按文件名顺序原子移动至 `processing/`，再解析 JSON → 返回 Intent。
  - `acknowledge(intentId)`：处理成功时将对应文件移动到 `processed/`。
  - `reject(intentId, reason)`：验证失败时将文件移动到 `failed/` 并写入 `<name>.reason`。
- Plugin 写入规则（来自对方协议文件《IDEAL_CITY_CRYSTALTECH_PROTOCOL.md》）：
  - 先写临时文件 `.intent_xxx.json` → `fsync` → `os.replace` 到 `pending/`，确保 Forge 只看到完整文件。
  - 文件名默认 `<intent_id>.json`，Intent ID 全局唯一。
  - 根对象需包含 `player_id`（UUID 字符串）与 `intent`（Manifestation Intent 载体）两部分：
    ```json
    {
      "player_id": "5a1f8c4e-3e30-4a7e-8b1b-d8b0c7e9c0de",
      "intent": { /* Manifestation Intent 字段 */ }
    }
    ```
  - `intent` 对象字段遵循第 1.1 节示例，可选携带 `metadata`、`notes` 等扩展字段；Forge 解析时忽略未知键。
- 示例与工具：
  - `city-intents/pending/example-stage-1.json`：插件提供的示例内容；实际投递前需外层包裹 `player_id` + `intent` 结构。
  - `scripts/drop_intent_example.py`：插件提供的原子写入脚本（读取 `IDEAL_CITY_PROTOCOL_ROOT`），Forge 团队可在本地或 CI 快速生成任意 stage 的示例。
- 联调与回归请执行《IDEAL_CITY_FORGE_CHECKLIST.md》中的步骤，确保从投递到阶段推进全链路通过。

> 注意：目录是唯一传输介质；Forge 不再暴露 HTTP/WebSocket 入口。如需新形态需双方共同修订协议。

### 1.4 Telemetry 与回执数据
- **Energy Snapshot**：新增 `ProtocolTelemetryService` 基于服务器 Tick 汇总玩家阶段、在线人数与昼夜节律生成能耗快照，并调用 `TechnologyStatusWriter.updateEnergy(...)` 写入 `technology-status.json` 的 `energy` 字段。
- **风险枚举**：同一服务检查 `city-intents/pending/` 的积压量，按阈值将 `intent_backlog`、`energy_grid_strain` 等条目写入 `risks` 队列，供 CityPhone 解释异常。
- **长周期社会稿件**：`SocialFeedWriter` 在写入单条事件文件的同时追加 JSON 行至 `cityphone/social-feed/events.jsonl`，便于插件端进行 24 小时窗口校验。
- **刷新频率**：Telemetry 每 30 秒在服务器线程执行一次，遇到 Stage 推进会立即触发额外写回，顶层 `updated_at` 随之刷新。

## 2. Forge 行为流程
1. **监听**：主线程定时或事件驱动调用 `ManifestationIntentService#consume()`。
2. **验证**：
   - 校验 `intentId` 是否为协议允许值
   - `allowedStage <= currentStage + 1`
   - 检查 `constraints`（如 `no_stage_skip`）
   - 确认玩家或世界上下文存在
  - 若 `expiresAt` 存在且 `now > expiresAt` → 记录 `intent_expired`，忽略该 Intent
3. **记账**：记录玩家可用的 `allowedStage`（例如存入 Capability）
4. **等待显化行为**：
   - 工作台/机器监听时，先检测玩家阶段是否有 **Intent 解锁**
   - 只有在 Intent 与行为空间都满足时才推进 Stage
5. **推进 Stage**：调用 `CrystalStageApi.tryAdvance()`，并重置对应 Intent（防止重复使用）
6. **日志**：
   - Intent 接收日志
   - 合法性校验日志
   - Stage 推进结果日志

## 3. 系统组件拆解
### 3.1 Intent 存储
- `PlayerIntentCapability`
  - 字段：`highestAllowedStage`（来自 Intent）
  - 方法：`updateFromIntent(ManifestationIntent intent)`
  - 注意：不直接推进 Stage，仅记录允许范围

### 3.2 Intent 消费服务
- `ManifestationIntentService`
  - `consumeForPlayer(UUID playerId)`
    - 从 Inbox 拉取 Intent
    - 校验后写入 `PlayerIntentCapability`
    - 若 Intent 过期：调用 `ManifestationIntentLogger.logExpired(intent)` 并丢弃
  - 保证线程安全（主线程调用）

### 3.3 行为监听扩展
- 工作台监听（现有 `StageProgressionEvents`）：
  1. 检查玩家 `highestAllowedStage`
  2. 仅当 `allowedStage >= 1` 且配方匹配时推进
- 未来机器监听同理，检查 Intent 条件

### 3.4 约束执行
- 若 Intent `constraints` 包含 `low_energy_only`：
  - 禁止高能配方，直到收到新的 Intent
- 可扩展为策略枚举：`ConstraintPolicy`

## 4. 日志与审计
- `ManifestationIntentLogger`
  - `logReceived(intent)`
  - `logRejected(intent, reason)`
  - `logApplied(player, stage)`
  - `logExpired(intent)`
- 全部写入 Forge 日志，配合世界保存进行审计

## 5. 错误处理
- 解析失败：记录并丢弃，不影响现有流程
- Intent 不合法：记录 `WARN`，不推进，也不回退
- Intent 过期：记录 `intent_expired`，不推进，也不回退
- 长时间未收到 Intent：阶段保持锁定（默认行为）

## 6. 开发待办（Forge 侧）
1. 搭建协议包结构 `com.crystaltech.protocol`
2. 实现 `ManifestationIntent` POJO 与 Codec
3. 定义 `ManifestationIntentInbox` 接口及文件/网络实现
4. 新建 `PlayerIntentCapability`
5. 在玩家进入世界时同步 Intent 能力
6. 修改 `StageProgressionEvents`，在推进前检查 Intent 允许值
7. 集成日志模块
8. 编写 GameTest：
   - 无 Intent → 配方不推进 Stage
   - Intent + 配方 → Stage 推进
   - Intent 仅提升一级 → 禁止跳级

## 7. 配置与可视化
- `config/crystaltech-protocol.toml`
  - 开关：启用/禁用协议模式
  - Inbox 类型：`file`, `http`, `mock`
  - 拉取周期 / 网络配置
- 不提供玩家 UI 提示，遵循 CityPhone 原则：世界变化即反馈

## 8. 联调要求
- 与 Plugin 团队确认 Intent JSON schema 与传输介质
- 制定 Mock 数据样例供 Forge 自测
- 规划联合演示：
  1. Plugin 产生 Intent
  2. Forge 接收并记录
  3. 玩家执行配方推进 Stage

## 9. 验收 checklist
- Forge 在无 Plugin 时：
  - Stage 不会自动提升
  - 手动注入 Intent 才能推进
- 合法 Intent + 行为双重满足 → Stage 提升成功
- 非法 Intent 或缺少 Intent → Stage 保持
- 日志记录完整，具备追溯性

## 10. 注意事项
- 所有 Stage 推进入口必须经过 Manifestation Intent 校验层，不得直接调用 Stage API。
- 任何绕过 Intent 的调试代码禁止合并到主干
- Intent 只用一次，重复使用需 Plugin 重发新的 Intent
- 将协议相关代码隔离，避免污染核心 Stage 逻辑

## 11. 多设备生态设计原则（Forge 侧关注重点）
### 11.1 核心隐喻
- Ideal City Plugin / CityPhone：理解与记录设备（副终端）
- CrystalTech Forge：主操作环境（主终端 / 世界工作站）
- 玩家并非切换系统，而是在同一世界内切换不同“设备”层级。

### 11.2 设备职责
- **Forge（主设备）**：承载方块、物品、机器、能量等高带宽交互，是技术显化与 Stage 真实存在的地方。
- **CityPhone（副设备）**：非常驻、需主动打开，只用于查看状态、研究裁决、理解世界反馈；绝不执行显化操作。

### 11.3 交互规则
1. **不抢主窗口**：CityPhone 不得自动弹出或阻断 Forge 操作，所有关键操作必须在 Forge 完成。
2. **操作 / 理解分离**：
  - 操作、试错、显化 → Forge 负责，并通过静默反馈（锁定/灰态）体现限制。
  - 理解、记录、裁决解释 → CityPhone 负责，呈现“城市系统记录”而非对话。
3. **无 AI 对话感**：CityPhone 使用档案式语言，避免第二人称提示，不模拟聊天。

### 11.4 UX 判据
- 玩家直觉：Forge 是世界本身；CityPhone 是理解世界的窗口；不打开 CityPhone 世界仍运转。
- 若玩家感到在填表、与 AI 对话或被 UI 推流程，则需回滚设计。

### 11.5 扩展意义
- 该模型支持未来接入多条技术线或多种 CityPhone 终端，而无需重构核心体验。

### 11.6 Forge 侧行动要点
- 确保所有阶段推进、配方、机器操作都在 Forge 世界内完成。
- CityPhone 仅通过协议提供状态解释，不返回“解锁”提示。
- 任何 UI 开发遵循“电脑改变世界，手机解释世界”的宪法。
