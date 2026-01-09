# CrystalTech · 紫水晶技术线落地文档

## 0. 术语约定
- **Stage**：技术阶段，使用 `CrystalStage` API 管理。
- **FE**：Forge Energy。
- **占位 json**：只创建格式正确的空配方文件，留待合作方补充实际输入输出。

## 1. 范围与目标
- 使用极少的新物品构建一条不可逆、可扩展的技术主线。
- 所有阶段推进均通过配方或机器产出触发 `CrystalStageApi.advance()`。
- 禁止再次使用右键触发、手持物品触发等方式推进阶段。

## 2. 阶段设计
| 阶段 | 名称           | 推进方式                     | 关键节点                                           |
| ---- | -------------- | ---------------------------- | -------------------------------------------------- |
| 0    | 紫水晶基线     | 原版采集                     | 开放原版紫水晶相关方块与掉落获取                  |
| 1    | 紫水晶材料化   | 工作台配方                   | 合成紫水晶粉、石英粉；完成后允许进入 Stage 2 配方 |
| 2    | 紫晶工业化     | 高炉 / 机器产出              | 制作紫晶合金锭；解锁功能方块                      |
| 3    | 紫晶系统化（预留） | 多方块结构、物流与能量联动 | 紫晶熔炉多方块→能量接口→高级合金                 |

## 3. Stage 规则与 API 对接
- `CrystalStageApi`：
  - 暴露 `advance(ServerPlayer player, CrystalStage stage)`。
  - 每个阶段对应唯一触发条件（参见配方清单）。
- 触发点：
  1. 工作台配方完成后监听 `PlayerEvent.ItemCraftedEvent`。
  2. 高炉 / 机器完成后在 `BlockEntity` tick 逻辑中判定输出槽。
- 数据存储：
  - 使用 `Capability` 存储玩家当前阶段。
  - 写入 `PlayerPersisted` NBT，确保下线保留。

## 4. 物品注册（List #1）
- 注册入口：`com.crystaltech.registry.ModItems`。
- 采用 `DeferredRegister<Item>`；命名空间 `crystaltech`。
- 物品列表：
  | ID                    | 中文名     | 英文名            | 用途说明                       |
  | --------------------- | ---------- | ----------------- | ------------------------------ |
  | `amethyst_powder`     | 紫水晶粉   | Amethyst Powder   | Stage 1 基础材料               |
  | `quartz_powder`       | 石英粉     | Quartz Powder     | Stage 1 辅料，用于合金前处理   |
  | `amethyst_alloy_ingot`| 紫晶合金锭 | Amethyst Alloy Ingot | Stage 2 核心工业材料        |
- 资源要求：
  - 模型：`assets/crystaltech/models/item/<id>.json`，父类 `item/generated`。
  - 纹理：`assets/crystaltech/textures/item/<id>.png`。
  - 语言：
    - `assets/crystaltech/lang/zh_cn.json`
    - `assets/crystaltech/lang/en_us.json`
- 额外 Tag：预留 `forge:ingots/amethyst_alloy`。

## 5. 工作台配方（List #2）
- 目录：`data/crystaltech/recipes/`。
- 占位文件：
  1. `amethyst_powder_from_shard.json`
  2. `quartz_powder_from_quartz.json`
- 配方格式：Shapeless Crafting (`minecraft:crafting_shapeless`)。
- 阶段钩子：
  - 完成上述任一配方 → 检查玩家阶段为 Stage 0 → 调用 `advance(Stage 1)`。

## 6. 高炉 / 工业配方（List #3）
- 目录：`data/crystaltech/recipes/industrial/`。
- 占位文件：`amethyst_alloy_ingot_smelting.json`（可同时提供熔炉与高炉版本）。
- 暂定配方：输入 `amethyst_powder` + `quartz_powder` → 输出 `amethyst_alloy_ingot`。
- 阶段钩子：
  - 熔炼输出槽检测到 `amethyst_alloy_ingot` 首次获取 → 当玩家阶段为 Stage 1 → 调用 `advance(Stage 2)`。

## 7. 工具与武器（List #4）
- 注册：`ModItems` 中新增 `AMETHYST_ALLOY_PICKAXE`、`AMETHYST_ALLOY_SWORD`（第二个可选）。
- 材质：使用自定义 `Tiers`，基础属性：
  - 耐久：比铁高 20%，比钻石低。
  - 挖掘速度：介于铁与钻石之间。
  - 攻击力加成：对齐铁 → 不追求战斗优势。
- 配方：暂挂 Stage 2；通过 `amethyst_alloy_ingot` + 原版棒组合。
- 解锁条件：仅 Stage 2 玩家可合成（依赖自定义配方条件 `stage_unlocked`）。

## 8. 方块注册（List #5）
- 注册入口：`com.crystaltech.registry.ModBlocks`，`DeferredRegister<Block>`。
- 方块：
  1. `amethyst_furnace`
     - `BlockBehaviour.Properties`：参考原版高炉，增加能量接口。
     - 搭配 `BlockItem`。
  2. `amethyst_bricks`
     - 普通装饰方块 + CTM 预留。
- 资源：
  - 纹理放至 `assets/crystaltech/textures/block/`。
  - 方块状态与模型：
    - `amethyst_furnace`：`blockstates/amethyst_furnace.json`（含 `lit` 状态）。
    - `amethyst_bricks`：基础方块模型，预留 CTM。

## 9. 材质连接（List #6）
- `amethyst_bricks`：
  - 提供基础 16×16 纹理。
  - 预留 `assets/crystaltech/textures/block/ctm/amethyst_bricks/`。
- `amethyst_furnace`：
  - `front`（熄灭）、`front_on`（点燃）、`side`、`top`、`bottom`。
  - 支持 `lit` 属性切换。
- 后续接入 CTM 时，计划使用 `Forge CTM` 或自研材质管线。

## 10. 多方块结构（List #7）
- 结构：
  ```
  B B B
  B F B
  B B B
  ```
  - `B`：`amethyst_bricks`
  - `F`：`amethyst_furnace`（核心块，需朝向玩家）
- 识别逻辑：
  1. `AmethystFurnaceBlockEntity` 在 `setPlacedBy` 或 Tick 中扫描 1 格半径。
  2. 检查 8 个邻居是否为 `amethyst_bricks`。
  3. 成功后设置 `isMultiblockFormed` 标记，解锁高级配方。
- 阶段推进：Stage 2 玩家首次搭建成功 → 自动提示 Stage 3 可用（暂不开放）。

## 11. 方块实体与能量系统（List #8）
- `AmethystFurnaceBlockEntity`：
  - 继承 `AbstractFurnaceBlockEntity` 或自定义容器。
  - 槽位：`input`、`aux`、`output`、`energy`。
  - Tick 逻辑：
    1. 检查能量是否满足消耗（占位值 40 FE / tick）。
    2. 消耗输入和能量，生成输出。
    3. 同步 `lit` 状态与客户端粒子。
  - 能量 Capability：`ForgeCapabilities.ENERGY`，内部实现 `EnergyStorage`。
- 同步与 UI：
  - 使用 `Menu` + `Screen` 显示能量条与进度。
  - 暂存 GUI 占位贴图。

## 12. 物流系统与线缆（List #9）
- 方块：`amethyst_flux_cable`。
- 属性：
  - 低传输量（例如 200 FE/t），但带可视化发光。
  - 使用 `BlockEntity` + `IItemHandler` / `IEnergyStorage` 推送 FE。
- 材质：半透明 + 自发光层。
- 逻辑：
  - 每 tick 扫描相邻方块，向需要 FE 的节点等量分发。
  - 保持短距离（例如最大连接 8 格）。
- 目标：强调“水晶神经网络”概念，而非全自动物流。

## 13. 配方占位实现步骤
1. 在 `data/crystaltech/recipes/` 写入占位 json，结构完整但使用明显的假输入（例如 `"ingredient": { "item": "minecraft:stone" }`）。
2. 在代码中针对占位配方禁用掉落（例如检测配方 ID 后返回）。
3. 待合作伙伴提供正式数据后替换。

## 14. 测试计划（参考 `docs/testing-checklist.md`）
- 单人测试流程：
  1. 新建存档 → 验证 Stage 0 采集紫水晶。
  2. 合成紫水晶粉 → 检查 Stage 1 是否解锁。
  3. 完成工业配方 → 检查 Stage 2。
  4. 搭建多方块 → 检查能量槽与高级配方开关。
- 自动化：编写 `GameTest` 覆盖 Stage 推进和 BlockEntity 能量逻辑。

## 15. 交付清单
- 代码：
  - `ModItems`, `ModBlocks`, `ModBlockEntities`, `ModMenus`。
  - `CrystalStageApi` 实现及阶段监听事件。
  - `AmethystFurnaceBlockEntity`、`AmethystFluxCableBlockEntity`。
- 资源：
  - 项目内所有 JSON、模型、纹理。
  - lang 文件（中英文）。
- 文档：
  - 本落地文档。
  - 更新 `docs/project-plan.md` 中的里程碑。

## 16. 后续路线（Stage 3 占位）
- 引入能量生成（`water_pulse_core`、事件供能）。
- 扩展多方块成套自动线。
- 构建 CTM 资源包并开箱即用。
