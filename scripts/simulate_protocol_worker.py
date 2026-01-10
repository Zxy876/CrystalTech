#!/usr/bin/env python3
"""Lightweight simulator that processes a single manifestation intent and writes protocol outputs.

This helper mimics the Forge-side pipeline sufficiently for end-to-end smoke tests when the
full mod runtime is not available.
"""

from __future__ import annotations

import argparse
import json
import shutil
import time
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Dict

STAGE_LABELS = {
    0: "baseline",
    1: "materialization",
    2: "stabilization",
}


def stage_label(stage: int) -> str:
    return STAGE_LABELS.get(stage, f"stage-{stage}")


def load_intent(path: Path) -> Dict[str, Any]:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def write_social_feed(root: Path,
                      player_id: str,
                      stage: int,
                      scenario_id: str | None,
                      scenario_version: str | None,
                      trust_index: float,
                      timestamp: datetime) -> None:
    social_dir = root / "cityphone" / "social-feed"
    social_dir.mkdir(parents=True, exist_ok=True)
    event = {
        "timestamp": timestamp.isoformat(),
        "event_type": "stage_advance",
        "stage": stage,
        "player_id": player_id,
        "scenario_id": scenario_id,
        "scenario_version": scenario_version,
        "trust_index": round(trust_index, 3),
    }
    event_name = f"{timestamp.strftime('%Y%m%dT%H%M%SZ')}-stage-{stage}.json"
    with (social_dir / event_name).open("w", encoding="utf-8") as handle:
        json.dump(event, handle, ensure_ascii=False, indent=2)
    trust_payload = {
        "timestamp": timestamp.isoformat(),
        "value": round(trust_index, 3),
    }
    with (social_dir / "trust_index.json").open("w", encoding="utf-8") as handle:
        json.dump(trust_payload, handle, ensure_ascii=False, indent=2)


def write_technology_status(root: Path,
                            stage: int,
                            scenario_id: str | None,
                            scenario_version: str | None,
                            player_id: str,
                            timestamp: datetime) -> None:
    status_path = root / "cityphone" / "technology-status.json"
    status_path.parent.mkdir(parents=True, exist_ok=True)
    event = {
        "type": "stage_manifested",
        "stage": stage,
        "label": stage_label(stage),
        "scenario_id": scenario_id,
        "scenario_version": scenario_version,
        "player_id": player_id,
        "player_name": player_id,
        "summary": f"Stage {stage} manifested by simulator",
        "occurred_at": timestamp.isoformat(),
    }
    payload = {
        "timestamp": timestamp.isoformat(),
        "updated_at": timestamp.isoformat(),
        "stage": {
            "current": stage,
            "level": stage,
            "label": stage_label(stage),
            "scenario_id": scenario_id,
            "scenario_version": scenario_version,
            "updated_at": timestamp.isoformat(),
            "source": "protocol-simulator",
        },
        "energy": {
            "status": "stable",
            "level": 75,
            "updated_at": timestamp.isoformat(),
            "generation": 120.0,
            "consumption": 95.0,
            "reserve": 40.0,
        },
        "risks": [],
        "alerts": [],
        "recent_events": [event],
    }
    with status_path.open("w", encoding="utf-8") as handle:
        json.dump(payload, handle, ensure_ascii=False, indent=2)


def append_manifestation_event(root: Path,
                               player_id: str,
                               intent_id: str,
                               stage: int,
                               timestamp: datetime) -> None:
    log_path = root / "manifestation_events.jsonl"
    log_path.parent.mkdir(parents=True, exist_ok=True)
    event = {
        "timestamp": timestamp.isoformat(),
        "event": "stage_manifested",
        "player_id": player_id,
        "intent_id": intent_id,
        "stage": stage,
        "source": "protocol-simulator",
    }
    with log_path.open("a", encoding="utf-8") as handle:
        json.dump(event, handle, ensure_ascii=False)
        handle.write("\n")


def process_intent(protocol_root: Path,
                   move_to_processing: bool = True,
                   trust_delta: float = 0.05) -> bool:
    city_intents = protocol_root / "city-intents"
    pending = city_intents / "pending"
    processing = city_intents / "processing"
    processed = city_intents / "processed"
    failed = city_intents / "failed"

    for directory in (pending, processing, processed, failed):
        directory.mkdir(parents=True, exist_ok=True)

    candidates = sorted(pending.glob("*.json"))
    if not candidates:
        return False

    source_path = candidates[0]
    target_processing = processing / source_path.name
    target_processed = processed / source_path.name

    if move_to_processing:
        shutil.move(str(source_path), target_processing)
        payload = load_intent(target_processing)
    else:
        payload = load_intent(source_path)

    envelope = payload if isinstance(payload, dict) else {}
    player_id = str(envelope.get("player_id") or "00000000-0000-0000-0000-000000000000")
    intent_payload = envelope.get("intent") if isinstance(envelope.get("intent"), dict) else {}
    intent_id = str(intent_payload.get("intent_id") or "unknown-intent")
    stage = int(intent_payload.get("allowed_stage") or 0)
    scenario_id = intent_payload.get("scenario_id")
    scenario_version = intent_payload.get("scenario_version")

    timestamp = datetime.now(timezone.utc)

    write_social_feed(protocol_root, player_id, stage, scenario_id, scenario_version, 0.5 + trust_delta * max(stage, 1), timestamp)
    write_technology_status(protocol_root, stage, scenario_id, scenario_version, player_id, timestamp)
    append_manifestation_event(protocol_root, player_id, intent_id, stage, timestamp)

    if move_to_processing:
        shutil.move(str(target_processing), target_processed)
    else:
        shutil.move(str(source_path), target_processed)

    return True


def main() -> None:
    parser = argparse.ArgumentParser(description="Simulate Forge protocol processing for tests.")
    parser.add_argument("protocol_root", type=Path, help="Path to the protocol root directory.")
    parser.add_argument("--trust-delta", type=float, default=0.05, help="Trust index delta applied when stage advances.")
    parser.add_argument("--poll-interval", type=float, default=1.0, help="Seconds between pending directory checks.")
    parser.add_argument("--timeout", type=float, default=120.0, help="Maximum seconds to wait for a pending intent.")
    args = parser.parse_args()

    protocol_root = args.protocol_root.resolve()
    start_time = time.time()
    while time.time() - start_time < args.timeout:
        if process_intent(protocol_root, trust_delta=args.trust_delta):
            print("Processed intent and wrote protocol outputs.")
            return
        time.sleep(args.poll_interval)

    raise SystemExit("Timed out waiting for pending intents.")


if __name__ == "__main__":
    main()
