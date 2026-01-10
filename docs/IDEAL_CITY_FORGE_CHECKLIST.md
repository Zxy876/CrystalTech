# 理想城 × 水晶科技 联调检查清单

此表用于 Forge 团队在不依赖 Plugin 运行的情况下，自检 Manifestation Intent 联调是否通过。

## 预备条件
- 已获得目录结构：`city-intents/pending|processing|processed|failed/`
- 拥有示例文件：`city-intents/pending/example-stage-1.json`（包含 `player_id` + `intent` 信封结构）
- 如需自动生成，可运行 `scripts/drop_intent_example.py`

## 验收步骤
- [ ] 将示例 intent 信封放入 `city-intents/pending/`
- [ ] Forge 日志打印 “Intent received” 或等效提示
- [ ] JSON schema 校验通过（字段名、类型、时间戳）
- [ ] 意图文件被移动至 `city-intents/processing/`
- [ ] 合法玩家行为触发阶段推进（Stage advance）
- [ ] 意图文件在完成后移入 `city-intents/processed/`
- [ ] 当目录为空时，Stage 不发生推进
- [ ] 过期意图被拒绝并移入 `city-intents/failed/`，同时输出拒绝原因

## Plugin 行为声明
- Plugin 仅在裁决 `ACCEPT` 且 ready_for_build 时写入 `pending/`；`INCOMPLETE` 或草稿状态不会生成意图。

## 常见问题提醒
- 若需要自定义 intent，请保持信封结构和 `player_id`、`intent.allowed_stage`、`intent.issued_at`、`intent.expires_at` 等字段完整，并使用 ISO-8601 UTC 时间。
- Forge 可根据自身流程将文件移动到 `processing/` 或 `failed/`，Plugin 不会回写这些目录。
