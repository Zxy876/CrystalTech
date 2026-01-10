package com.crystaltech.capability.intent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.crystaltech.protocol.ManifestationIntent;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

/**
 * Mutable capability data for player manifestation allowances.
 */
public final class PlayerIntent implements IPlayerIntent {
    private static final String KEY_ACTIVE_ALLOWED_STAGE = "ActiveAllowedStage";
    private static final String KEY_RECORDED_CEILING = "RecordedStageCeiling";
    private static final String KEY_INTENT_ID = "IntentId";
    private static final String KEY_SCENARIO_ID = "ScenarioId";
    private static final String KEY_SCENARIO_VERSION = "ScenarioVersion";
    private static final String KEY_CONSTRAINTS = "Constraints";
    private static final String KEY_CONTEXT_NOTES = "ContextNotes";
    private static final String KEY_EXPIRES_AT = "ExpiresAt";
    private static final String KEY_LAST_UPDATED = "LastUpdated";
    private static final String KEY_LAST_CONSUMED_ID = "LastConsumedIntentId";
    private static final String KEY_LAST_CONSUMED_AT = "LastConsumedAt";

    private int activeAllowedStage;
    private int recordedStageCeiling;
    private String intentId;
    private String scenarioId;
    private String scenarioVersion;
    private final List<String> constraints = new ArrayList<>();
    private final List<String> contextNotes = new ArrayList<>();
    private long expiresAtEpochSeconds = -1L;
    private long lastUpdatedEpochSeconds;
    private String lastConsumedIntentId;
    private long lastConsumedEpochSeconds;

    @Override
    public int getActiveAllowedStage() {
        return activeAllowedStage;
    }

    @Override
    public int getRecordedStageCeiling() {
        return recordedStageCeiling;
    }

    @Override
    public Optional<String> getActiveIntentId() {
        return Optional.ofNullable(intentId);
    }

    @Override
    public Optional<String> getScenarioId() {
        return Optional.ofNullable(scenarioId);
    }

    @Override
    public Optional<String> getScenarioVersion() {
        return Optional.ofNullable(scenarioVersion);
    }

    @Override
    public List<String> getConstraints() {
        return Collections.unmodifiableList(constraints);
    }

    @Override
    public List<String> getContextNotes() {
        return Collections.unmodifiableList(contextNotes);
    }

    @Override
    public Optional<Long> getActiveExpiryEpochSeconds() {
        return expiresAtEpochSeconds > 0 ? Optional.of(expiresAtEpochSeconds) : Optional.empty();
    }

    @Override
    public Optional<String> getLastConsumedIntentId() {
        return Optional.ofNullable(lastConsumedIntentId);
    }

    @Override
    public Optional<Long> getLastConsumedEpochSeconds() {
        return lastConsumedEpochSeconds > 0 ? Optional.of(lastConsumedEpochSeconds) : Optional.empty();
    }

    @Override
    public void updateFromIntent(ManifestationIntent intent, Instant now) {
        this.intentId = intent.intentId();
        this.scenarioId = intent.scenarioId();
        this.scenarioVersion = intent.scenarioVersion();
        this.activeAllowedStage = Math.max(0, intent.allowedStage());
        this.recordedStageCeiling = Math.max(recordedStageCeiling, activeAllowedStage);
        this.constraints.clear();
        this.constraints.addAll(intent.constraints());
        this.contextNotes.clear();
        this.contextNotes.addAll(intent.contextNotes());
        this.expiresAtEpochSeconds = intent.expiresAt() != null ? intent.expiresAt().getEpochSecond() : -1L;
        this.lastUpdatedEpochSeconds = now.getEpochSecond();
    }

    @Override
    public boolean isStageAllowed(int stage, Instant now) {
        return hasActiveIntent() && !isExpired(now) && activeAllowedStage >= stage;
    }

    @Override
    public boolean hasActiveIntent() {
        return intentId != null;
    }

    @Override
    public boolean isExpired(Instant now) {
        return expiresAtEpochSeconds > 0 && now.getEpochSecond() > expiresAtEpochSeconds;
    }

    @Override
    public void markConsumed(int stageReached, Instant now) {
        this.recordedStageCeiling = Math.max(recordedStageCeiling, stageReached);
        this.activeAllowedStage = Math.max(0, stageReached);
        this.lastConsumedIntentId = this.intentId;
        this.lastConsumedEpochSeconds = now.getEpochSecond();
        this.intentId = null;
        this.scenarioId = null;
        this.scenarioVersion = null;
        this.constraints.clear();
        this.contextNotes.clear();
        this.expiresAtEpochSeconds = -1L;
        this.lastUpdatedEpochSeconds = now.getEpochSecond();
    }

    @Override
    public void copyFrom(IPlayerIntent other) {
        this.activeAllowedStage = other.getActiveAllowedStage();
        this.recordedStageCeiling = other.getRecordedStageCeiling();
        this.intentId = other.getActiveIntentId().orElse(null);
        this.scenarioId = other.getScenarioId().orElse(null);
        this.scenarioVersion = other.getScenarioVersion().orElse(null);
        this.constraints.clear();
        this.constraints.addAll(other.getConstraints());
        this.contextNotes.clear();
        this.contextNotes.addAll(other.getContextNotes());
        this.expiresAtEpochSeconds = other.getActiveExpiryEpochSeconds().orElse(-1L);
        this.lastConsumedIntentId = other.getLastConsumedIntentId().orElse(null);
        this.lastConsumedEpochSeconds = other.getLastConsumedEpochSeconds().orElse(0L);
        this.lastUpdatedEpochSeconds = Instant.now().getEpochSecond();
    }

    public CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(KEY_ACTIVE_ALLOWED_STAGE, activeAllowedStage);
        tag.putInt(KEY_RECORDED_CEILING, recordedStageCeiling);
        if (intentId != null) {
            tag.putString(KEY_INTENT_ID, intentId);
        }
        if (scenarioId != null) {
            tag.putString(KEY_SCENARIO_ID, scenarioId);
        }
        if (scenarioVersion != null) {
            tag.putString(KEY_SCENARIO_VERSION, scenarioVersion);
        }
        if (!constraints.isEmpty()) {
            tag.put(KEY_CONSTRAINTS, writeStringList(constraints));
        }
        if (!contextNotes.isEmpty()) {
            tag.put(KEY_CONTEXT_NOTES, writeStringList(contextNotes));
        }
        if (expiresAtEpochSeconds > 0) {
            tag.putLong(KEY_EXPIRES_AT, expiresAtEpochSeconds);
        }
        if (lastUpdatedEpochSeconds > 0) {
            tag.putLong(KEY_LAST_UPDATED, lastUpdatedEpochSeconds);
        }
        if (lastConsumedIntentId != null) {
            tag.putString(KEY_LAST_CONSUMED_ID, lastConsumedIntentId);
        }
        if (lastConsumedEpochSeconds > 0) {
            tag.putLong(KEY_LAST_CONSUMED_AT, lastConsumedEpochSeconds);
        }
        return tag;
    }

    public void deserialize(CompoundTag tag) {
        this.activeAllowedStage = tag.getInt(KEY_ACTIVE_ALLOWED_STAGE);
        this.recordedStageCeiling = tag.getInt(KEY_RECORDED_CEILING);
        this.intentId = tag.contains(KEY_INTENT_ID) ? tag.getString(KEY_INTENT_ID) : null;
        this.scenarioId = tag.contains(KEY_SCENARIO_ID) ? tag.getString(KEY_SCENARIO_ID) : null;
        this.scenarioVersion = tag.contains(KEY_SCENARIO_VERSION) ? tag.getString(KEY_SCENARIO_VERSION) : null;
        this.constraints.clear();
        this.contextNotes.clear();
        if (tag.contains(KEY_CONSTRAINTS, net.minecraft.nbt.Tag.TAG_LIST)) {
            readStringList(tag.getList(KEY_CONSTRAINTS, net.minecraft.nbt.Tag.TAG_STRING), constraints);
        }
        if (tag.contains(KEY_CONTEXT_NOTES, net.minecraft.nbt.Tag.TAG_LIST)) {
            readStringList(tag.getList(KEY_CONTEXT_NOTES, net.minecraft.nbt.Tag.TAG_STRING), contextNotes);
        }
        this.expiresAtEpochSeconds = tag.contains(KEY_EXPIRES_AT) ? tag.getLong(KEY_EXPIRES_AT) : -1L;
        this.lastUpdatedEpochSeconds = tag.contains(KEY_LAST_UPDATED) ? tag.getLong(KEY_LAST_UPDATED) : 0L;
        this.lastConsumedIntentId = tag.contains(KEY_LAST_CONSUMED_ID) ? tag.getString(KEY_LAST_CONSUMED_ID) : null;
        this.lastConsumedEpochSeconds = tag.contains(KEY_LAST_CONSUMED_AT) ? tag.getLong(KEY_LAST_CONSUMED_AT) : 0L;
    }

    private static ListTag writeStringList(List<String> values) {
        ListTag list = new ListTag();
        for (String value : values) {
            list.add(StringTag.valueOf(value));
        }
        return list;
    }

    private static void readStringList(ListTag listTag, List<String> target) {
        for (int i = 0; i < listTag.size(); i++) {
            target.add(listTag.getString(i));
        }
    }
}
