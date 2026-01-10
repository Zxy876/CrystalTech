package com.crystaltech.protocol;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable representation of a manifestation intent originating from the Ideal City plugin.
 */
public final class ManifestationIntent {
    private final String intentId;
    private final String schemaVersion;
    private final String scenarioId;
    private final String scenarioVersion;
    private final int allowedStage;
    private final String confidenceLevel;
    private final List<String> constraints;
    private final List<String> contextNotes;
    private final Instant issuedAt;
    private final Instant expiresAt;
    private final String signature;

    public ManifestationIntent(String intentId,
                               String schemaVersion,
                               String scenarioId,
                               String scenarioVersion,
                               int allowedStage,
                               String confidenceLevel,
                               List<String> constraints,
                               List<String> contextNotes,
                               Instant issuedAt,
                               Instant expiresAt,
                               String signature) {
        this.intentId = Objects.requireNonNull(intentId, "intentId");
        this.schemaVersion = Objects.requireNonNull(schemaVersion, "schemaVersion");
        this.scenarioId = Objects.requireNonNull(scenarioId, "scenarioId");
        this.scenarioVersion = scenarioVersion;
        this.allowedStage = allowedStage;
        this.confidenceLevel = Objects.requireNonNull(confidenceLevel, "confidenceLevel");
        this.constraints = List.copyOf(constraints == null ? Collections.emptyList() : constraints);
        this.contextNotes = List.copyOf(contextNotes == null ? Collections.emptyList() : contextNotes);
        this.issuedAt = Objects.requireNonNull(issuedAt, "issuedAt");
        this.expiresAt = expiresAt;
        this.signature = Objects.requireNonNull(signature, "signature");
    }

    public String intentId() {
        return intentId;
    }

    public String schemaVersion() {
        return schemaVersion;
    }

    public String scenarioId() {
        return scenarioId;
    }

    public String scenarioVersion() {
        return scenarioVersion;
    }

    public int allowedStage() {
        return allowedStage;
    }

    public String confidenceLevel() {
        return confidenceLevel;
    }

    public List<String> constraints() {
        return constraints;
    }

    public List<String> contextNotes() {
        return contextNotes;
    }

    public Instant issuedAt() {
        return issuedAt;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public boolean hasExpiry() {
        return expiresAt != null;
    }

    public boolean isExpired(Instant now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    public String signature() {
        return signature;
    }

    @Override
    public String toString() {
        return "ManifestationIntent{" +
                "intentId='" + intentId + '\'' +
                ", schemaVersion='" + schemaVersion + '\'' +
                ", scenarioId='" + scenarioId + '\'' +
                ", scenarioVersion='" + scenarioVersion + '\'' +
                ", allowedStage=" + allowedStage +
                ", confidenceLevel='" + confidenceLevel + '\'' +
                ", constraints=" + constraints +
                ", contextNotes=" + contextNotes +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", signaturePresent=" + (signature != null && !signature.isBlank()) +
                '}';
    }
}
