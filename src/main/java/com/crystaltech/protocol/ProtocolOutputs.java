package com.crystaltech.protocol;

import java.util.Objects;

/**
 * Central registry for protocol-side writers shared across subsystems.
 */
public final class ProtocolOutputs {
    private static volatile SocialFeedWriter socialFeedWriter = SocialFeedWriter.disabled();
    private static volatile TechnologyStatusWriter technologyStatusWriter = TechnologyStatusWriter.disabled();

    private ProtocolOutputs() {
    }

    public static void install(SocialFeedWriter feedWriter, TechnologyStatusWriter statusWriter) {
        socialFeedWriter = Objects.requireNonNull(feedWriter);
        technologyStatusWriter = Objects.requireNonNull(statusWriter);
    }

    public static SocialFeedWriter socialFeedWriter() {
        return socialFeedWriter;
    }

    public static TechnologyStatusWriter technologyStatusWriter() {
        return technologyStatusWriter;
    }
}
