"""CityPhone social feedback ingestion from Forge-provided artefacts."""

from __future__ import annotations

import json
from datetime import datetime, timezone
from pathlib import Path
from typing import Iterable, List, Literal, Optional

from pydantic import BaseModel, ConfigDict, Field

SocialFeedbackCategory = Literal["praise", "concern", "controversy", "regulation_proposal"]


class SocialFeedbackEntry(BaseModel):
    entry_id: str
    category: SocialFeedbackCategory
    title: str
    body: str
    issued_at: datetime
    stage: Optional[int] = None
    trust_delta: float = 0.0
    stress_delta: float = 0.0
    tags: List[str] = Field(default_factory=list)

    model_config = ConfigDict(ser_json_encoders={datetime: lambda value: value.isoformat()})


class SocialFeedbackSnapshot(BaseModel):
    entries: List[SocialFeedbackEntry] = Field(default_factory=list)
    trust_index: float = 0.0
    stress_index: float = 0.0
    updated_at: Optional[datetime] = None

    model_config = ConfigDict(ser_json_encoders={datetime: lambda value: value.isoformat()})


class SocialFeedbackRepository:
    """Read Forge-authored social feedback feeds for CityPhone presentation."""

    def __init__(self, protocol_root: Path) -> None:
        self._root = protocol_root / "cityphone" / "social-feed"
        self._root.mkdir(parents=True, exist_ok=True)
        self._events_file = self._root / "events.jsonl"
        self._metrics_file = self._root / "metrics.json"

    def load_snapshot(self, *, limit: int = 20) -> SocialFeedbackSnapshot:
        entries = list(self._load_entries(limit))
        metrics = self._load_metrics()
        updated_at = metrics.get("updated_at")
        if updated_at is None and entries:
            updated_at = max(entry.issued_at for entry in entries)
        return SocialFeedbackSnapshot(
            entries=entries,
            trust_index=float(metrics.get("trust_index", 0.0)),
            stress_index=float(metrics.get("stress_index", 0.0)),
            updated_at=updated_at,
        )

    def _load_entries(self, limit: int) -> Iterable[SocialFeedbackEntry]:
        if not self._events_file.exists():
            return []
        entries: List[SocialFeedbackEntry] = []
        with self._events_file.open("r", encoding="utf-8") as handle:
            for line in handle:
                if limit and len(entries) >= limit:
                    break
                text = line.strip()
                if not text:
                    continue
                try:
                    payload = json.loads(text)
                except json.JSONDecodeError:
                    continue
                entry = self._build_entry(payload)
                if entry is None:
                    continue
                entries.append(entry)
        entries.sort(key=lambda item: item.issued_at, reverse=True)
        return entries

    def _build_entry(self, payload: dict) -> Optional[SocialFeedbackEntry]:
        try:
            issued_at = self._coerce_datetime(payload.get("issued_at"))
            if issued_at is None:
                return None
            entry = SocialFeedbackEntry(
                entry_id=str(payload.get("entry_id") or payload.get("id") or ""),
                category=self._resolve_category(payload.get("category")),
                title=str(payload.get("title") or ""),
                body=str(payload.get("body") or payload.get("summary") or ""),
                issued_at=issued_at,
                stage=self._maybe_int(payload.get("stage")),
                trust_delta=float(payload.get("trust_delta", 0.0) or 0.0),
                stress_delta=float(payload.get("stress_delta", 0.0) or 0.0),
                tags=self._collect_tags(payload.get("tags")),
            )
        except Exception:
            return None
        if not entry.entry_id:
            return None
        if entry.category is None:
            return None
        if not entry.title:
            return None
        return entry

    def _load_metrics(self) -> dict:
        if not self._metrics_file.exists():
            return {}
        try:
            payload = json.loads(self._metrics_file.read_text(encoding="utf-8"))
        except json.JSONDecodeError:
            return {}
        metrics: dict = {}
        if "trust_index" in payload:
            metrics["trust_index"] = float(payload.get("trust_index", 0.0) or 0.0)
        if "stress_index" in payload:
            metrics["stress_index"] = float(payload.get("stress_index", 0.0) or 0.0)
        updated_raw = payload.get("updated_at")
        dt_value = self._coerce_datetime(updated_raw)
        if dt_value is not None:
            metrics["updated_at"] = dt_value
        return metrics

    def _coerce_datetime(self, value: object) -> Optional[datetime]:
        if isinstance(value, datetime):
            return value
        if isinstance(value, (int, float)):
            return datetime.fromtimestamp(value, tz=timezone.utc)
        if isinstance(value, str):
            try:
                parsed = datetime.fromisoformat(value)
            except ValueError:
                return None
            if parsed.tzinfo is None:
                parsed = parsed.replace(tzinfo=timezone.utc)
            return parsed
        return None

    def _resolve_category(self, value: object) -> Optional[SocialFeedbackCategory]:
        if not isinstance(value, str):
            return None
        lowered = value.lower()
        mapping = {
            "praise": "praise",
            "concern": "concern",
            "controversy": "controversy",
            "regulation_proposal": "regulation_proposal",
            "regulation-proposal": "regulation_proposal",
        }
        return mapping.get(lowered)

    def _maybe_int(self, value: object) -> Optional[int]:
        if isinstance(value, int):
            return value
        if isinstance(value, float):
            return int(value)
        if isinstance(value, str) and value.strip():
            try:
                return int(value)
            except ValueError:
                return None
        return None

    def _collect_tags(self, value: object) -> List[str]:
        if not value:
            return []
        if isinstance(value, str):
            return [value]
        if isinstance(value, Iterable):
            result: List[str] = []
            for item in value:
                text = str(item).strip()
                if text:
                    result.append(text)
            return result
        return []
