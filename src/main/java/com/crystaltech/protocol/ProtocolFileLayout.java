package com.crystaltech.protocol;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves shared protocol file-system locations from environment overrides.
 */
public final class ProtocolFileLayout {
    private static final String ENV_PROTOCOL_ROOT = "CRYSTALTECH_PROTOCOL_ROOT";
    private static final String SYS_PROTOCOL_ROOT = "crystaltech.protocol.root";

    private final Path protocolRoot;
    private final Path inboxRoot;
    private final Path eventLogFile;
    private final Path cityphoneRoot;
    private final Path socialFeedDir;
    private final Path technologyStatusFile;

    private ProtocolFileLayout(Path protocolRoot, Path inboxRoot, Path eventLogFile) {
        this(protocolRoot,
                inboxRoot,
                eventLogFile,
                protocolRoot.resolve("cityphone"),
                protocolRoot.resolve("cityphone").resolve("social-feed"),
                protocolRoot.resolve("cityphone").resolve("technology-status.json"));
    }

    private ProtocolFileLayout(Path protocolRoot,
                               Path inboxRoot,
                               Path eventLogFile,
                               Path cityphoneRoot,
                               Path socialFeedDir,
                               Path technologyStatusFile) {
        this.protocolRoot = Objects.requireNonNull(protocolRoot);
        this.inboxRoot = Objects.requireNonNull(inboxRoot);
        this.eventLogFile = Objects.requireNonNull(eventLogFile);
        this.cityphoneRoot = Objects.requireNonNull(cityphoneRoot);
        this.socialFeedDir = Objects.requireNonNull(socialFeedDir);
        this.technologyStatusFile = Objects.requireNonNull(technologyStatusFile);
    }

    public static ProtocolFileLayout resolve(Path gameDirectory) {
        Path configuredRoot = resolveConfiguredRoot().orElse(gameDirectory.resolve("city-intents"));
        Path normalisedRoot = configuredRoot.toAbsolutePath().normalize();

        boolean endsWithCityIntents = normalisedRoot.getFileName() != null && normalisedRoot.getFileName().toString().equals("city-intents");

        Path inbox = endsWithCityIntents ? normalisedRoot : normalisedRoot.resolve("city-intents");
        Path protocolRoot = endsWithCityIntents && normalisedRoot.getParent() != null ? normalisedRoot.getParent() : normalisedRoot;
        Path eventLog = protocolRoot.resolve("manifestation_events.jsonl");
        Path cityphoneRoot = protocolRoot.resolve("cityphone");
        Path socialFeedDir = cityphoneRoot.resolve("social-feed");
        Path technologyStatusFile = cityphoneRoot.resolve("technology-status.json");

        return new ProtocolFileLayout(protocolRoot, inbox, eventLog, cityphoneRoot, socialFeedDir, technologyStatusFile);
    }

    public Path protocolRoot() {
        return protocolRoot;
    }

    public Path inboxRoot() {
        return inboxRoot;
    }

    public Path eventLogFile() {
        return eventLogFile;
    }

    public Path cityphoneRoot() {
        return cityphoneRoot;
    }

    public Path socialFeedDir() {
        return socialFeedDir;
    }

    public Path technologyStatusFile() {
        return technologyStatusFile;
    }

    private static Optional<Path> resolveConfiguredRoot() {
        String sysProperty = System.getProperty(SYS_PROTOCOL_ROOT);
        if (sysProperty != null && !sysProperty.isBlank()) {
            return Optional.of(Path.of(sysProperty.trim()));
        }
        String env = System.getenv(ENV_PROTOCOL_ROOT);
        if (env != null && !env.isBlank()) {
            return Optional.of(Path.of(env.trim()));
        }
        return Optional.empty();
    }
}
