package com.crystaltech.protocol;

import java.util.Optional;

/**
 * Abstraction representing the source of manifestation intents delivered by the Ideal City plugin.
 */
public interface ManifestationIntentInbox {

    Optional<Message> poll();

    void acknowledge(String intentId);

    void reject(String intentId, String reason);

    static ManifestationIntentInbox empty() {
        return new ManifestationIntentInbox() {
            @Override
            public Optional<Message> poll() {
                return Optional.empty();
            }

            @Override
            public void acknowledge(String intentId) {
                // no-op
            }

            @Override
            public void reject(String intentId, String reason) {
                // no-op
            }
        };
    }

    /**
     * Lightweight envelope describing a player-targeted intent.
     */
    record Message(java.util.UUID playerId, ManifestationIntent intent, String rawIntentJson) {
    }
}
