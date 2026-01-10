import json
from datetime import datetime, timezone
from pathlib import Path

from app.core.ideal_city.social_feedback import (
    SocialFeedbackRepository,
    SocialFeedbackSnapshot,
)


def _write_events(path: Path, entries: list[dict]) -> None:
    with path.open("w", encoding="utf-8") as handle:
        for entry in entries:
            handle.write(json.dumps(entry, ensure_ascii=False))
            handle.write("\n")


def test_social_feedback_repository_empty(tmp_path: Path) -> None:
    repo = SocialFeedbackRepository(tmp_path)
    snapshot = repo.load_snapshot()
    assert isinstance(snapshot, SocialFeedbackSnapshot)
    assert snapshot.entries == []
    assert snapshot.trust_index == 0.0
    assert snapshot.stress_index == 0.0


def test_social_feedback_repository_parses_entries(tmp_path: Path) -> None:
    repo = SocialFeedbackRepository(tmp_path)
    events_dir = tmp_path / "cityphone" / "social-feed"
    events_dir.mkdir(parents=True, exist_ok=True)
    events_file = events_dir / "events.jsonl"
    metrics_file = events_dir / "metrics.json"

    now = datetime(2026, 1, 10, 12, 0, tzinfo=timezone.utc)
    _write_events(
        events_file,
        [
            {
                "entry_id": "event-1",
                "category": "praise",
                "title": "社区赞扬",
                "body": "社区称赞最新的节能措施。",
                "issued_at": now.isoformat(),
                "trust_delta": 3.5,
                "stress_delta": -1.0,
                "stage": 2,
                "tags": ["energy", "community"],
            },
            {
                "id": "event-2",
                "category": "concern",
                "title": "夜间噪音担忧",
                "summary": "部分居民抱怨夜间噪音增加。",
                "issued_at": (now.replace(hour=11)).isoformat(),
                "trust_delta": -2,
                "stress_delta": 4,
                "tags": "noise",
            },
        ],
    )

    metrics_file.write_text(
        json.dumps(
            {
                "trust_index": 42.5,
                "stress_index": 18.0,
                "updated_at": now.isoformat(),
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )

    snapshot = repo.load_snapshot(limit=5)

    assert len(snapshot.entries) == 2
    assert snapshot.entries[0].entry_id == "event-1"
    assert snapshot.entries[0].tags == ["energy", "community"]
    assert snapshot.entries[1].entry_id == "event-2"
    assert snapshot.entries[1].tags == ["noise"]
    assert snapshot.trust_index == 42.5
    assert snapshot.stress_index == 18.0
    assert snapshot.updated_at == now
