#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LINK_PATH="$ROOT_DIR/run/city-intents"
DEFAULT_PLUGIN_PATH="$ROOT_DIR/../DRIFT_SCIENCELINE/backend/data/ideal_city/protocol/city-intents"
TARGET_PATH="${CITY_INTENT_PLUGIN_PATH:-$DEFAULT_PLUGIN_PATH}"

if [[ ! -d "$TARGET_PATH" ]]; then
  echo "[city-intents] Target inbox directory not found: $TARGET_PATH" >&2
  echo "Set CITY_INTENT_PLUGIN_PATH to the plugin's city-intents directory before launching." >&2
  exit 1
fi

mkdir -p "$TARGET_PATH/pending" "$TARGET_PATH/processing" "$TARGET_PATH/processed" "$TARGET_PATH/failed"

if [[ -L "$LINK_PATH" ]]; then
  CURRENT_TARGET="$(readlink "$LINK_PATH")"
  if [[ "$CURRENT_TARGET" != "$TARGET_PATH" ]]; then
    echo "[city-intents] Updating symlink: $LINK_PATH -> $TARGET_PATH"
    ln -sfn "$TARGET_PATH" "$LINK_PATH"
  fi
elif [[ -e "$LINK_PATH" ]]; then
  TIMESTAMP="$(date +%s)"
  BACKUP_PATH="${LINK_PATH}.backup.${TIMESTAMP}"
  echo "[city-intents] Backing up existing inbox to $BACKUP_PATH"
  mv "$LINK_PATH" "$BACKUP_PATH"
  ln -s "$TARGET_PATH" "$LINK_PATH"
  echo "[city-intents] Created symlink: $LINK_PATH -> $TARGET_PATH"
else
  ln -s "$TARGET_PATH" "$LINK_PATH"
  echo "[city-intents] Created symlink: $LINK_PATH -> $TARGET_PATH"
fi
