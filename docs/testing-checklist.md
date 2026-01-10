# CrystalTech 技线测试清单

## 1. 准备
- 启动开发客户端：`./gradlew runClient`
- 进入新建的测试世界（建议创造模式，命名随意）
- 创建世界时在 `更多世界选项` 中开启 **允许作弊**；若是已有世界，可通过暂停菜单 → `对局域网开放` → 打开 **允许作弊** 临时启用
- 打开调试日志窗口，方便查看阶段变化日志

## 2. 基础校验
- **Mod 列表**：主菜单 `Mods` 中确认 `CrystalTech 0.2.0-techline` 存在并显示描述
- **世界加载**：创建世界确保无崩溃或报错弹窗

## 3. 阶段推进测试
1. 通过指令给予初始材料：
   - `/give @s minecraft:amethyst_shard 5`（若提示未知指令，确认已开启作弊/获得管理员权限）
2. 切换为生存模式（测试物品消耗）：`/gamemode survival`
3. **申请 Manifestation Intent（命令调试）**：
   - 执行 `/crystalintent grant 1`
   - 期望：终端输出 Intent 记录日志（`Received manifestation intent ...`）
   - `/data get entity @s ForgeCaps['crystaltech:player_intent']` 可查看允许阶段信息
   - （可选）手动在 `city-intents/pending/` 投放 JSON 文件验证文件 Inbox：内容遵循文档示例
4. **阶段 0 → 阶段 1（工作台推进）**：
   - 在工作台放入 1 个 `minecraft:amethyst_shard`（或 `minecraft:quartz`），合成对应粉末
   - 结果：获得 `Amethyst Powder` 或 `Quartz Powder`
   - 验证：
      - 控制台日志出现阶段变更调试信息与 `Applied manifestation allowance`
      - `/data get entity @s ForgeCaps['crystaltech:crystal_stage']` 返回 `1`
5. **重复触发保护**：
   - 保持阶段 1（无新的 Intent），再次合成粉末
   - 期望：阶段保持 1，日志提示 `manifestation_intent_missing`
6. **阶段 1 → 阶段 2（占位，Milestone 2 完成后执行）**：
   - 待紫晶熔炉实装后，通过机器输出验证推进逻辑
   - 当前版本：确认日志提示“未实现”或无推进

## 4. 人物死亡持久化
- 使用 `/kill @s` 或主动死亡
- 重生后执行 `/data get entity @s ForgeCaps['crystaltech:crystal_stage']`
- 期望：阶段保持 1（当前可达最高阶段），物品不自动返还（需手动回收）

## 5. 能力克隆校验
- 通过 `/effect give @s minecraft:instant_health 255 10 true` 等手段制造克隆事件（可在死亡后检查）
- 确认重生玩家仍处在之前的阶段（预期为阶段 1）

## 6. 客户端关闭与日志
- `Ctrl+C` 停止 `runClient`
- 检查终端输出，确认无异常堆栈

## 7. 测试记录
- 在项目 issue 或笔记中记录测试日期、版本、结果及发现的问题
