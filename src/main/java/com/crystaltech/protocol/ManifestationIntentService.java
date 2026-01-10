package com.crystaltech.protocol;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.crystaltech.capability.intent.PlayerIntentCapability;
import com.crystaltech.core.CrystalStageApi;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Pulls manifestation intents from an inbox and applies them to player capabilities.
 */
public final class ManifestationIntentService {
    private static final ManifestationIntentService INSTANCE = new ManifestationIntentService();
    private static final Set<String> DEFAULT_SCHEMA_VERSIONS = Set.of("0.1.0");

    private volatile ManifestationIntentInbox inbox = ManifestationIntentInbox.empty();
    private volatile IntentSignatureValidator signatureValidator = IntentSignatureValidator.permissive("unsigned_not_configured");
    private volatile ManifestationEventWriter eventWriter = ManifestationEventWriter.disabled();
    private volatile Set<String> acceptedSchemaVersions = DEFAULT_SCHEMA_VERSIONS;
    private final Map<UUID, ManifestationIntent> queued = new ConcurrentHashMap<>();

    private ManifestationIntentService() {
    }

    public static ManifestationIntentService getInstance() {
        return INSTANCE;
    }

    public void registerInbox(ManifestationIntentInbox inbox) {
        configure(new Configuration(
                Objects.requireNonNull(inbox),
                signatureValidator,
                eventWriter,
                acceptedSchemaVersions));
    }

    public void configure(Configuration configuration) {
        Objects.requireNonNull(configuration);
        this.inbox = Objects.requireNonNull(configuration.inbox());
        this.signatureValidator = Objects.requireNonNull(configuration.signatureValidator());
        this.eventWriter = Objects.requireNonNull(configuration.eventWriter());
        this.acceptedSchemaVersions = normaliseSchemaVersions(configuration.acceptedSchemaVersions());
    }

    public void submitDebugIntent(UUID playerId, ManifestationIntent intent) {
        queued.put(playerId, intent);
    }

    /**
     * Consumes intents for any players currently online.
     */
    public void consume(Instant now) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        ManifestationIntentInbox localInbox = this.inbox;
        // drain at most a safe number of messages per tick to prevent infinite loops
        for (int i = 0; i < 64; i++) {
            Optional<ManifestationIntentInbox.Message> messageOpt = localInbox.poll();
            if (messageOpt.isEmpty()) {
                break;
            }
            ManifestationIntentInbox.Message message = messageOpt.get();
            ProcessingOutcome outcome = processInboxMessage(server, message, now);
            switch (outcome.state()) {
                case ACCEPTED -> localInbox.acknowledge(message.intent().intentId());
                case IGNORED -> localInbox.acknowledge(message.intent().intentId());
                case REJECTED -> localInbox.reject(message.intent().intentId(), outcome.reason());
            }
        }

        if (queued.isEmpty()) {
            return;
        }

        queued.forEach((uuid, intent) -> processDebugIntent(server, uuid, intent, now));
        queued.clear();
    }

    private ProcessingOutcome processInboxMessage(MinecraftServer server, ManifestationIntentInbox.Message message, Instant now) {
        ManifestationIntent intent = message.intent();
        if (!acceptedSchemaVersions.contains(intent.schemaVersion())) {
            ManifestationIntentLogger.logRejected(intent, message.playerId(), "schema_version_unsupported");
            eventWriter.recordIntentRejected(message.playerId(), intent, "schema_version_unsupported", "not_verified", now);
            return ProcessingOutcome.rejected("schema_version_unsupported");
        }

        IntentSignatureValidator.ValidationResult sigResult = signatureValidator.validate(intent, message.rawIntentJson());
        if (!sigResult.valid()) {
            ManifestationIntentLogger.logRejected(intent, message.playerId(), sigResult.status());
            eventWriter.recordIntentRejected(message.playerId(), intent, sigResult.status(), sigResult.status(), now);
            return ProcessingOutcome.rejected(sigResult.status());
        }

        return processIntent(server, message.playerId(), intent, now, sigResult.status());
    }

    private void processDebugIntent(MinecraftServer server, UUID playerId, ManifestationIntent intent, Instant now) {
        processIntent(server, playerId, intent, now, "debug");
    }

    private ProcessingOutcome processIntent(MinecraftServer server,
                                            UUID playerId,
                                            ManifestationIntent intent,
                                            Instant now,
                                            String signatureStatus) {
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player == null) {
            ManifestationIntentLogger.logIgnored(intent, playerId, "player_offline");
            eventWriter.recordIntentIgnored(playerId, intent, "player_offline", now);
            return ProcessingOutcome.ignored("player_offline");
        }

        if (intent.isExpired(now)) {
            ManifestationIntentLogger.logExpired(intent, playerId, now);
            eventWriter.recordIntentRejected(playerId, intent, "intent_expired", signatureStatus, now);
            return ProcessingOutcome.rejected("intent_expired");
        }

        var lazyIntent = player.getCapability(PlayerIntentCapability.CAPABILITY);
        var capability = lazyIntent.orElse(null);
        if (capability == null) {
            ManifestationIntentLogger.logRejected(intent, playerId, "capability_missing");
            eventWriter.recordIntentRejected(playerId, intent, "capability_missing", signatureStatus, now);
            return ProcessingOutcome.rejected("capability_missing");
        }

        int currentStage = CrystalStageApi.getStage(player);
        if (intent.allowedStage() > currentStage + 1) {
            ManifestationIntentLogger.logRejected(intent, playerId, "no_stage_skip");
            eventWriter.recordIntentRejected(playerId, intent, "no_stage_skip", signatureStatus, now);
            return ProcessingOutcome.rejected("no_stage_skip");
        }
        if (intent.allowedStage() <= currentStage) {
            ManifestationIntentLogger.logIgnored(intent, playerId, "stage_already_reached");
            eventWriter.recordIntentIgnored(playerId, intent, "stage_already_reached", now);
            return ProcessingOutcome.ignored("stage_already_reached");
        }
        capability.updateFromIntent(intent, now);
        ManifestationIntentLogger.logReceived(intent, playerId);
        eventWriter.recordIntentAccepted(playerId, intent, signatureStatus, now);
        return ProcessingOutcome.accepted();
    }

    public void markStageConsumed(Player player, int reachedStage, Instant now) {
        player.getCapability(PlayerIntentCapability.CAPABILITY).ifPresent(capability -> {
            String intentId = capability.getActiveIntentId().orElse(null);
            String scenarioId = capability.getScenarioId().orElse(null);
            String scenarioVersion = capability.getScenarioVersion().orElse(null);
            capability.markConsumed(reachedStage, now);
            eventWriter.recordStageManifested(player.getUUID(), player.getGameProfile().getName(), intentId, scenarioId, scenarioVersion, reachedStage, now);
            ProtocolOutputs.socialFeedWriter().recordStageAdvance(player.getUUID(), player.getGameProfile().getName(), scenarioId, scenarioVersion, reachedStage, now);
            TechnologyStatusWriter statusWriter = ProtocolOutputs.technologyStatusWriter();
            statusWriter.updateStage(reachedStage, scenarioId, scenarioVersion, now);
            statusWriter.recordStageManifest(player.getUUID(), player.getGameProfile().getName(), scenarioId, scenarioVersion, reachedStage, now);
        });
    }

    private Set<String> normaliseSchemaVersions(Collection<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return DEFAULT_SCHEMA_VERSIONS;
        }
        return Collections.unmodifiableSet(Set.copyOf(versions));
    }

    public record Configuration(ManifestationIntentInbox inbox,
                                IntentSignatureValidator signatureValidator,
                                ManifestationEventWriter eventWriter,
                                Collection<String> acceptedSchemaVersions) {
    }

    private enum OutcomeState {
        ACCEPTED,
        IGNORED,
        REJECTED
    }

    private record ProcessingOutcome(OutcomeState state, String reason) {
        static ProcessingOutcome accepted() {
            return new ProcessingOutcome(OutcomeState.ACCEPTED, null);
        }

        static ProcessingOutcome ignored(String reason) {
            return new ProcessingOutcome(OutcomeState.IGNORED, reason);
        }

        static ProcessingOutcome rejected(String reason) {
            return new ProcessingOutcome(OutcomeState.REJECTED, reason);
        }
    }
}
