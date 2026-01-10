package com.crystaltech.capability.intent;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.crystaltech.protocol.ManifestationIntent;

/**
 * Describes the manifestation intent state tracked for a player.
 */
public interface IPlayerIntent {
    int getActiveAllowedStage();

    int getRecordedStageCeiling();

    Optional<String> getActiveIntentId();

    Optional<String> getScenarioId();

    Optional<String> getScenarioVersion();

    List<String> getConstraints();

    List<String> getContextNotes();

    Optional<Long> getActiveExpiryEpochSeconds();

    Optional<String> getLastConsumedIntentId();

    Optional<Long> getLastConsumedEpochSeconds();

    void updateFromIntent(ManifestationIntent intent, Instant now);

    boolean isStageAllowed(int stage, Instant now);

    boolean hasActiveIntent();

    boolean isExpired(Instant now);

    void markConsumed(int stageReached, Instant now);

    void copyFrom(IPlayerIntent other);
}
