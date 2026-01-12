"""Tests for Ideal City worldview-aware adjudication."""

from __future__ import annotations

import json
import os
from datetime import datetime, timezone
from pathlib import Path
from importlib import reload

import pytest

from app.core.ideal_city import scenario_repository as scenario_module
from app.core.ideal_city import worldview as worldview_module
from app.core.ideal_city.adjudication_contract import VerdictEnum
from app.core.ideal_city.build_plan import PlayerPose
from app.core.ideal_city.pipeline import DeviceSpecSubmission, IdealCityPipeline
from app.core.ideal_city.pipeline_registry import reset_pipeline
from app.core.ideal_city.story_state import StoryMilestoneUpdate, StoryStatePatch
from app.core.ideal_city.story_templates import build_patch_for_milestones, get_template_catalog
from common.protocols.story_state import coerce_story_state_patch


@pytest.fixture(autouse=True)
def ideal_city_data_root(tmp_path, monkeypatch):
    data_root = tmp_path / "ideal_city"
    data_root.mkdir()
    protocol_root = tmp_path / "protocol"
    protocol_root.mkdir()

    worldview = {
        "spirit_core": "发明是一种对真实社会问题的负责回应，而非效率竞赛。",
        "historical_context": {"touchstones": ["社会尚未工业化，机械与结构仍在摸索阶段"]},
        "player_role": {"identity": "社会请来的创造回应者"},
        "design_principles": ["先理解世界问题，再动手建造"],
        "forbidden_patterns": ["只追求效率或炫技而忽略社会后果"],
        "review_questions": ["你正在回应的具体社会问题是什么？"],
        "response_styles": {
            "affirm": ["工坊长老们认可此项发明的长期照应价值。"],
            "reject": ["城市档案员指出：缺少对社会矛盾的明晰描述，无法归档。"],
            "follow_up": [
                "请补全至少一条被确认的世界限制。",
                "请提交逐步验证逻辑的过程说明。",
            ],
        },
    }
    scenario = {
        "scenario_id": "default",
        "title": "熄灯区的公共工坊",
        "problem_statement": "熄灯区缺乏安全工坊空间，居民无法修缮工具。",
        "contextual_constraints": ["夜间能源供应有限，噪音需受限"],
        "stakeholders": ["熄灯区居民委员会"],
        "emerging_risks": ["若维护计划缺失，工坊或成为安全隐患"],
        "success_markers": ["居民按周使用工坊且不发生事故"],
    }

    (data_root / "worldview.json").write_text(
        json.dumps(worldview, ensure_ascii=False), encoding="utf-8"
    )
    scenarios_dir = data_root / "scenarios"
    scenarios_dir.mkdir()
    (scenarios_dir / "default.json").write_text(
        json.dumps(scenario, ensure_ascii=False), encoding="utf-8"
    )

    monkeypatch.setenv("IDEAL_CITY_DATA_ROOT", str(data_root))
    monkeypatch.setenv("IDEAL_CITY_PROTOCOL_ROOT", str(protocol_root))
    worldview_module._cached_worldview = None
    worldview_module._cached_path = None
    reset_pipeline()
    reload(scenario_module)
    yield
    monkeypatch.delenv("IDEAL_CITY_DATA_ROOT", raising=False)
    monkeypatch.delenv("IDEAL_CITY_PROTOCOL_ROOT", raising=False)
    worldview_module._cached_worldview = None
    worldview_module._cached_path = None
    reset_pipeline()


def test_single_sentence_submission_auto_structures():
    pipeline = IdealCityPipeline()
    submission = DeviceSpecSubmission(
        player_id="tester",
        narrative="我要搭建一个工坊",
        scenario_id="default",
    )
    result = pipeline.submit(submission)
    body_text = "\n".join(result.notice.body)
    assert result.ruling.verdict == VerdictEnum.INCOMPLETE
    assert result.spec.world_constraints == []
    assert result.spec.logic_outline == []
    assert result.spec.resource_ledger == []
    assert result.spec.risk_register == []
    assert result.spec.success_criteria == []
    assert "Context cues:" in result.notice.body
    assert any(line.startswith(" ~ 世界精神") for line in result.notice.body)
    assert "仍停留" in body_text
    state = pipeline.cityphone_state("tester", "default")
    assert state.appendix.plan.available is False
    assert state.unknowns
    assert state.technology_status is not None
    assert result.build_plan is None
    assert result.uncertainty is not None
    assert result.uncertainty.probes == 1
    assert "世界约束" in result.uncertainty.stable_missing
    assert result.uncertainty.unstable_missing == []
    assert any("自检结果：仍缺失" in line for line in result.notice.body)
    assert result.research_hint is not None
    assert result.notice.research_hint == result.research_hint
    assert result.research_hint.startswith("档案状态：研究进行中。")
    assert "核心缺口" in result.research_hint
    assert state.appendix.plan.status in {"未就绪", "pending"}
    assert result.manifestation_intent is None
    pending_dir = Path(os.environ["IDEAL_CITY_PROTOCOL_ROOT"]) / "city-intents" / "pending"
    assert pending_dir.exists()
    assert not any(pending_dir.glob("*.json"))


def test_partial_logic_attempt_marked_unstable():
    pipeline = IdealCityPipeline()
    submission = DeviceSpecSubmission(
        player_id="tester",
        narrative="社区需要一个工坊，我已经招募志愿者并规划了第一步。",
        scenario_id="default",
        logic_outline=["整理旧工具"],
    )
    result = pipeline.submit(submission)
    assert result.ruling.verdict == VerdictEnum.INCOMPLETE
    assert result.uncertainty is not None
    assert "执行步骤" in result.uncertainty.unstable_missing
    assert "世界约束" in result.uncertainty.stable_missing
    assert any("自检结果：尝试填写但仍未通过" in line for line in result.notice.body)
    assert result.research_hint is not None
    assert "梳理中要素" in result.research_hint
    state = pipeline.cityphone_state("tester", "default")
    assert state.unknowns
    assert state.technology_status is not None
    assert result.manifestation_intent is None
    pending_dir = Path(os.environ["IDEAL_CITY_PROTOCOL_ROOT"]) / "city-intents" / "pending"
    assert pending_dir.exists()
    assert not any(pending_dir.glob("*.json"))


def test_acceptance_mentions_affirmation():
    pipeline = IdealCityPipeline()
    submission = DeviceSpecSubmission(
        player_id="tester",
        narrative="我要搭建一个工坊，为熄灯区提供安全的夜间维修空间，并安排志愿者轮值守护，确保居民可以安心使用。",
        scenario_id="default",
        world_constraints=["遵守夜间能源限制"],
        logic_outline=["整理材料", "邀请邻里", "布置安全检查"],
        resource_ledger=["木材与工具 - 社区工坊", "照明设备 - 夜间供电站"],
        risk_register=["风险: 夜间噪音扰民 / 安装隔音帘"],
        success_criteria=["居民按周排班使用", "夜间事故为零"],
        player_pose=PlayerPose(world="world", x=10.0, y=65.0, z=-4.0, yaw=0.0, pitch=0.0),
        location_hint="工坊南侧平台",
    )
    result = pipeline.submit(submission)
    assert result.ruling.verdict == VerdictEnum.ACCEPT
    assert any("工坊长老们认可" in line for line in result.notice.body)
    assert any(line.startswith(" ~ 世界精神") for line in result.notice.body)
    state = pipeline.cityphone_state("tester", "default")
    assert state.city_interpretation
    assert state.appendix.plan.available is True
    assert not state.unknowns[:1]
    assert state.technology_status is not None
    assert result.build_plan is not None
    assert result.manifestation_intent is not None
    intent = result.manifestation_intent
    assert intent.allowed_stage == 1
    assert intent.scenario_id == "default"
    assert "no_stage_skip" in intent.constraints
    assert intent.expires_at > intent.issued_at
    pending_dir = Path(os.environ["IDEAL_CITY_PROTOCOL_ROOT"]) / "city-intents" / "pending"
    assert pending_dir.exists()
    intent_files = sorted(pending_dir.glob("*.json"))
    assert len(intent_files) == 1
    payload = json.loads(intent_files[0].read_text(encoding="utf-8"))
    assert payload.get("player_id") == "tester"
    intent_payload = payload.get("intent", {})
    assert intent_payload.get("intent_id") == intent.intent_id
    assert intent_payload.get("intent_kind") == intent.intent_kind
    assert intent_payload.get("scenario_id") == "default"
    metadata = intent_payload.get("metadata", {})
    assert metadata.get("source_spec_id") == result.spec.spec_id
    audit_path = Path(os.environ["IDEAL_CITY_PROTOCOL_ROOT"]) / "city-intents" / "intent_audit.jsonl"
    assert audit_path.exists()
    audit_lines = [line for line in audit_path.read_text(encoding="utf-8").splitlines() if line.strip()]
    assert audit_lines, "Audit log should receive the emitted intent"
    audit_entry = json.loads(audit_lines[-1])
    assert audit_entry.get("player_id") == "tester"
    audit_intent = audit_entry.get("intent", {})
    assert audit_intent.get("intent_id") == intent.intent_id
    data_root = Path(os.environ["IDEAL_CITY_DATA_ROOT"])  # provided by fixture
    queue_file = data_root / "build_queue" / "build_queue.jsonl"
    assert queue_file.exists()
    queue_lines = queue_file.read_text(encoding="utf-8").strip().splitlines()
    assert queue_lines, "Build plan queue should contain the generated plan"


def test_mod_refresh_intent_triggers_reload(tmp_path, monkeypatch):
    mods_root = tmp_path / "mods"
    mods_root.mkdir()
    monkeypatch.setenv("IDEAL_CITY_MODS_ROOT", str(mods_root))
    pipeline = IdealCityPipeline()
    manifest_dir = mods_root / "idealcity.sample"
    manifest_dir.mkdir()
    manifest = {
        "mod_id": "idealcity:sample",
        "name": "Sample Mod",
        "version": "0.1.0",
        "assets": {
            "schematics": [],
            "structures": [],
            "scripts": [],
            "textures": [],
        },
    }
    (manifest_dir / "mod.json").write_text(json.dumps(manifest, ensure_ascii=False), encoding="utf-8")
    submission = DeviceSpecSubmission(
        player_id="tester",
        narrative="请帮我刷新模组缓存",
        scenario_id="default",
    )
    result = pipeline.submit(submission)
    assert result.ruling.verdict == VerdictEnum.ACCEPT
    assert result.notice.broadcast is not None
    mods = pipeline.list_mods()
    assert any(entry.get("mod_id") == "idealcity:sample" for entry in mods)
    monkeypatch.delenv("IDEAL_CITY_MODS_ROOT", raising=False)


def test_missing_structure_falls_back_to_rule_based_reject(monkeypatch):
    monkeypatch.setenv("IDEAL_CITY_AI_DISABLE", "1")
    pipeline = IdealCityPipeline()
    submission = DeviceSpecSubmission(
        player_id="tester",
        narrative="我想建设一个社区中心，提供夜间自习和手工活动，但目前还没有准备详细计划。",
        scenario_id="default",
        world_constraints=[],
        logic_outline=["只有一个想法"],
        risk_register=[],
    )
    result = pipeline.submit(submission)
    monkeypatch.delenv("IDEAL_CITY_AI_DISABLE", raising=False)
    assert result.ruling.verdict == VerdictEnum.INCOMPLETE
    body_text = "\n".join(result.notice.body)
    assert "仍停留在对《" in body_text or "提案仍在梳理问题" in body_text
    assert any("需补" in line or "阻塞" in line for line in result.notice.body)


def test_single_turn_high_logic_unlocks_plan(ideal_city_data_root):
    pipeline = IdealCityPipeline()
    submission = DeviceSpecSubmission(
        player_id="logic_master",
        narrative=(
            "我将协调社区工匠与供电站搭建一个夜间工坊，"
            "确保先整理旧工具、安装隔音装置，再引导居民试运行。"
        ),
        scenario_id="default",
        world_constraints=["遵守夜间能源限制", "夜间噪音控制"],
        logic_outline=["整理旧工具", "搭设主体结构", "安排试运行"],
        resource_ledger=["木材与隔音材料 - 社区工坊", "照明设备 - 夜间供电站"],
        success_criteria=["居民按周排班使用", "夜间事故为零"],
        risk_register=["风险: 夜间照明不足 / 增配移动灯带"],
        player_pose=PlayerPose(world="world", x=20.0, y=70.0, z=5.0, yaw=45.0, pitch=0.0),
        location_hint="熄灯区广场东侧",
    )
    pipeline.submit(submission)
    state = pipeline.cityphone_state("logic_master", "default")
    assert state.ready_for_build is True
    assert state.build_capability >= 120
    assert state.logic_score >= 90
    assert state.blocking == []
    assert state.panels.plan.available is True
    assert any("计划已排队" in line for line in state.panels.plan.pending_reasons)
    assert state.technology_status is not None


def test_single_turn_high_motivation_missing_logic_blocks_plan(ideal_city_data_root):
    pipeline = IdealCityPipeline()
    long_story = (
        "我太想马上建设社区中心了，居民们一直在夜里找我希望有个安全的地方，"
        "所以我准备亲自筹划活动、号召邻里、联系志愿者，还会拍摄记录分享，"
        "只要大家愿意参加，我就会一直更新计划，绝不让这个梦想落空。"
    )
    submission = DeviceSpecSubmission(
        player_id="motivation_only",
        narrative=long_story,
        scenario_id="default",
        world_constraints=[],
        logic_outline=["整理旧物"],
        risk_register=[],
    )
    pipeline.submit(submission)
    state = pipeline.cityphone_state("motivation_only", "default")
    assert state.ready_for_build is False
    assert state.logic_score < 100
    assert state.build_capability < 120
    assert state.blocking
    assert any("风险登记" in item for item in state.blocking)
    assert state.panels.plan.available is False
    assert state.panels.plan.pending_reasons
    assert state.technology_status is not None


def test_draft_submission_requests_manual_review(monkeypatch):
    monkeypatch.setenv("IDEAL_CITY_AI_DISABLE", "1")
    pipeline = IdealCityPipeline()
    submission = DeviceSpecSubmission(
        player_id="tester",
        narrative="这是一份草稿，需要人工确认",
        scenario_id="default",
        is_draft=True,
        world_constraints=["遵守夜间能源限制"],
        logic_outline=["整理材料", "邀请邻里"],
        risk_register=["噪音扰民"],
        success_criteria=["居民按周排班使用"],
    )
    result = pipeline.submit(submission)
    monkeypatch.delenv("IDEAL_CITY_AI_DISABLE", raising=False)
    assert result.ruling.verdict == VerdictEnum.REVIEW_REQUIRED
    assert any("草稿" in line for line in result.notice.body)


def test_executed_plan_feedback_updates_cityphone(ideal_city_data_root):
    pipeline = IdealCityPipeline()
    submission = DeviceSpecSubmission(
        player_id="builder",
        narrative=(
            "我将协调社区工匠搭建夜间工坊，"
            "完成隔音与照明部署，确保居民轮值安全运行。"
        ),
        scenario_id="default",
        world_constraints=["遵守夜间能源限制", "夜间噪音控制"],
        logic_outline=["整理旧工具", "搭设主体结构", "安排试运行"],
        resource_ledger=["木材与隔音材料 - 社区工坊", "照明设备 - 夜间供电站"],
        success_criteria=["居民按周排班使用", "夜间事故为零"],
        risk_register=["风险: 夜间噪音扰民 / 安装隔音帘"],
        player_pose=PlayerPose(world="world", x=12.0, y=68.0, z=-8.0, yaw=10.0, pitch=5.0),
        location_hint="熄灯区广场东入口",
    )
    result = pipeline.submit(submission)
    assert result.build_plan is not None

    plan_id = str(result.build_plan.plan_id)
    executed_dir = Path(os.environ["IDEAL_CITY_DATA_ROOT"]) / "build_queue" / "executed"
    executed_dir.mkdir(parents=True, exist_ok=True)
    executed_payload = {
        "summary": result.build_plan.summary,
        "status": "completed",
        "commands": ["/fill 0 64 0 1 65 1 stone"],
        "mod_hooks": [],
        "player_pose": {
            "world": "world",
            "x": 12.0,
            "y": 68.0,
            "z": -8.0,
            "yaw": 10.0,
            "pitch": 5.0,
        },
        "logged_at": "2026-01-07T11:00:00Z",
    }
    (executed_dir / f"{plan_id}.json").write_text(json.dumps(executed_payload, ensure_ascii=False), encoding="utf-8")

    state = pipeline.cityphone_state("builder", "default")

    assert state.panels.plan.available is True
    assert state.panels.plan.plan_id == plan_id
    assert state.panels.plan.status == "已完成"
    assert any("建造指令已派发" in note for note in state.panels.plan.pending_reasons)
    assert "builder" == state.player_id
    assert state.ready_for_build is False, "Completed execution clears readiness"
    assert state.technology_status is not None


def test_cityphone_state_reads_technology_status_file(ideal_city_data_root):
    pipeline = IdealCityPipeline()
    protocol_root = Path(os.environ["IDEAL_CITY_PROTOCOL_ROOT"])
    payload = {
        "stage": {"label": "阶段 3 — 社会反馈闭环", "level": 3},
        "energy": {"generation": 200.5, "consumption": 180.2, "capacity": 260.0, "buffer": 24.3},
        "risks": [{"risk_id": "grid", "level": "warning", "summary": "主干电网波动"}],
        "recent_events": [
            {
                "event_id": "evt-100",
                "category": "upgrade",
                "description": "完成城市级能量调配器安装",
                "occurred_at": "2026-01-10T08:00:00Z",
            }
        ],
        "updated_at": "2026-01-10T08:05:00Z",
    }
    (protocol_root / "technology-status.json").write_text(json.dumps(payload, ensure_ascii=False), encoding="utf-8")

    state = pipeline.cityphone_state("status_reader", "default")

    assert state.technology_status is not None
    snapshot = state.technology_status
    assert snapshot.stage is not None
    assert snapshot.stage.level == 3
    assert snapshot.energy is not None
    assert snapshot.energy.storage == 24.3
    assert snapshot.risk_alerts and snapshot.risk_alerts[0].risk_id == "grid"
    assert snapshot.recent_events and snapshot.recent_events[0].event_id == "evt-100"
    assert snapshot.updated_at == datetime(2026, 1, 10, 8, 5, tzinfo=timezone.utc)


def test_milestone_patch_catalog_autofills_state(ideal_city_data_root):
    catalog = get_template_catalog()
    assert "logic_from_npc" in catalog.milestones

    pipeline = IdealCityPipeline()

    raw_patch = build_patch_for_milestones(["logic_from_npc"], notes={"logic_from_npc": "测试备注"})
    assert raw_patch, "Patch payload should be generated from catalog"

    normalized = coerce_story_state_patch(raw_patch)
    milestone_raw = normalized.pop("milestones", {})
    milestone_updates = {
        key: StoryMilestoneUpdate(**payload) for key, payload in milestone_raw.items()
    }
    allowed_fields = set(StoryStatePatch.model_fields.keys())
    patch_kwargs = {key: value for key, value in normalized.items() if key in allowed_fields}
    story_patch = StoryStatePatch(**patch_kwargs, milestones=milestone_updates or None)

    pipeline.apply_story_patch(
        player_id="catalog_tester",
        scenario_id="default",
        patch=story_patch,
    )

    state = pipeline.cityphone_state("catalog_tester", "default")
    milestone_map = {entry.milestone_id: entry for entry in state.milestones}
    assert "logic_from_npc" in milestone_map
    milestone_entry = milestone_map["logic_from_npc"]
    assert milestone_entry.status == "complete"
    expected_source = catalog.milestones["logic_from_npc"].default_source or "npc"
    assert milestone_entry.source == expected_source
    assert milestone_entry.note == "测试备注"
    assert milestone_entry.templates == ["logic_quick_start"]
    assert state.templates_completed >= 1
