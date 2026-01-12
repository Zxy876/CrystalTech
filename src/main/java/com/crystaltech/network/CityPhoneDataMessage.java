package com.crystaltech.network;

import com.crystaltech.capability.intent.IPlayerIntent;
import com.crystaltech.capability.intent.PlayerIntentCapability;
import com.crystaltech.core.CrystalStageApi;
import com.crystaltech.client.CityPhoneClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.function.Supplier;

/**
 * Carries a snapshot of the player's CityPhone intent state to the client.
 */
public final class CityPhoneDataMessage {
    private final CityPhoneSnapshot snapshot;

    public CityPhoneDataMessage(CityPhoneSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public CityPhoneSnapshot snapshot() {
        return snapshot;
    }

    public static CityPhoneDataMessage fromPlayer(ServerPlayer player, Instant now) {
        int currentStage = CrystalStageApi.getStage(player);
        Optional<IPlayerIntent> intentOpt = player.getCapability(PlayerIntentCapability.CAPABILITY).resolve();
        if (intentOpt.isEmpty()) {
            return new CityPhoneDataMessage(CityPhoneSnapshot.empty(currentStage, now.getEpochSecond()));
        }
        IPlayerIntent intent = intentOpt.get();
        return new CityPhoneDataMessage(CityPhoneSnapshot.from(currentStage, intent, now.getEpochSecond()));
    }

    public static void encode(CityPhoneDataMessage message, FriendlyByteBuf buf) {
        CityPhoneSnapshot snapshot = message.snapshot;
        buf.writeInt(snapshot.currentStage());
        buf.writeInt(snapshot.recordedStageCeiling());
        buf.writeInt(snapshot.activeAllowedStage());
        buf.writeBoolean(snapshot.hasActiveIntent());
        buf.writeUtf(snapshot.intentId() == null ? "" : snapshot.intentId());
        buf.writeUtf(snapshot.scenarioId() == null ? "" : snapshot.scenarioId());
        buf.writeUtf(snapshot.scenarioVersion() == null ? "" : snapshot.scenarioVersion());
        buf.writeBoolean(snapshot.activeExpiryEpochSeconds().isPresent());
        snapshot.activeExpiryEpochSeconds().ifPresent(buf::writeLong);
        writeStringList(buf, snapshot.constraints());
        writeStringList(buf, snapshot.contextNotes());
        buf.writeBoolean(snapshot.lastConsumedIntentId() != null);
        if (snapshot.lastConsumedIntentId() != null) {
            buf.writeUtf(snapshot.lastConsumedIntentId());
            buf.writeLong(snapshot.lastConsumedEpochSeconds());
        }
        buf.writeLong(snapshot.generatedAtEpochSeconds());
    }

    public static CityPhoneDataMessage decode(FriendlyByteBuf buf) {
        int currentStage = buf.readInt();
        int recordedStageCeiling = buf.readInt();
        int activeAllowedStage = buf.readInt();
        boolean hasActiveIntent = buf.readBoolean();
        String intentId = emptyToNull(buf.readUtf());
        String scenarioId = emptyToNull(buf.readUtf());
        String scenarioVersion = emptyToNull(buf.readUtf());
        OptionalLong expiresAt;
        if (buf.readBoolean()) {
            expiresAt = OptionalLong.of(buf.readLong());
        } else {
            expiresAt = OptionalLong.empty();
        }
        List<String> constraints = readStringList(buf);
        List<String> contextNotes = readStringList(buf);
        String lastConsumedIntentId = null;
        long lastConsumedEpoch = 0L;
        if (buf.readBoolean()) {
            lastConsumedIntentId = buf.readUtf();
            lastConsumedEpoch = buf.readLong();
        }
        long generatedAt = buf.readLong();
        CityPhoneSnapshot snapshot = new CityPhoneSnapshot(
                currentStage,
                recordedStageCeiling,
                activeAllowedStage,
                hasActiveIntent,
                intentId,
                scenarioId,
                scenarioVersion,
                expiresAt,
                constraints,
                contextNotes,
                lastConsumedIntentId,
                lastConsumedEpoch,
                generatedAt
        );
        return new CityPhoneDataMessage(snapshot);
    }

    public static void handle(CityPhoneDataMessage message, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CityPhoneClient.openCityPhone(message.snapshot));
        ctx.setPacketHandled(true);
    }

    private static void writeStringList(FriendlyByteBuf buf, List<String> values) {
        buf.writeVarInt(values.size());
        for (String value : values) {
            buf.writeUtf(value);
        }
    }

    private static List<String> readStringList(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> values = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            values.add(buf.readUtf());
        }
        return values;
    }

    private static String emptyToNull(String value) {
        return value.isEmpty() ? null : value;
    }

    /**
     * Snapshot of the player's manifestation intent state.
     */
    public static final class CityPhoneSnapshot {
        private final int currentStage;
        private final int recordedStageCeiling;
        private final int activeAllowedStage;
        private final boolean hasActiveIntent;
        private final String intentId;
        private final String scenarioId;
        private final String scenarioVersion;
        private final OptionalLong activeExpiryEpochSeconds;
        private final List<String> constraints;
        private final List<String> contextNotes;
        private final String lastConsumedIntentId;
        private final long lastConsumedEpochSeconds;
        private final long generatedAtEpochSeconds;

        private CityPhoneSnapshot(int currentStage,
                                  int recordedStageCeiling,
                                  int activeAllowedStage,
                                  boolean hasActiveIntent,
                                  String intentId,
                                  String scenarioId,
                                  String scenarioVersion,
                                  OptionalLong activeExpiryEpochSeconds,
                                  List<String> constraints,
                                  List<String> contextNotes,
                                  String lastConsumedIntentId,
                                  long lastConsumedEpochSeconds,
                                  long generatedAtEpochSeconds) {
            this.currentStage = currentStage;
            this.recordedStageCeiling = recordedStageCeiling;
            this.activeAllowedStage = activeAllowedStage;
            this.hasActiveIntent = hasActiveIntent;
            this.intentId = intentId;
            this.scenarioId = scenarioId;
            this.scenarioVersion = scenarioVersion;
            this.activeExpiryEpochSeconds = activeExpiryEpochSeconds;
            this.constraints = List.copyOf(constraints);
            this.contextNotes = List.copyOf(contextNotes);
            this.lastConsumedIntentId = lastConsumedIntentId;
            this.lastConsumedEpochSeconds = lastConsumedEpochSeconds;
            this.generatedAtEpochSeconds = generatedAtEpochSeconds;
        }

        public static CityPhoneSnapshot from(int currentStage, IPlayerIntent intent, long generatedAtEpochSeconds) {
            OptionalLong expiry = intent.getActiveExpiryEpochSeconds()
                    .map(OptionalLong::of)
                    .orElseGet(OptionalLong::empty);
            return new CityPhoneSnapshot(
                    currentStage,
                    intent.getRecordedStageCeiling(),
                    intent.getActiveAllowedStage(),
                    intent.hasActiveIntent(),
                    intent.getActiveIntentId().orElse(null),
                    intent.getScenarioId().orElse(null),
                    intent.getScenarioVersion().orElse(null),
                    expiry,
                    intent.getConstraints(),
                    intent.getContextNotes(),
                    intent.getLastConsumedIntentId().orElse(null),
                    intent.getLastConsumedEpochSeconds().orElse(0L),
                    generatedAtEpochSeconds
            );
        }

        public static CityPhoneSnapshot empty(int currentStage, long generatedAtEpochSeconds) {
            return new CityPhoneSnapshot(currentStage,
                    currentStage,
                    currentStage,
                    false,
                    null,
                    null,
                    null,
                    OptionalLong.empty(),
                    List.of(),
                    List.of(),
                    null,
                    0L,
                    generatedAtEpochSeconds);
        }

        public int currentStage() {
            return currentStage;
        }

        public int recordedStageCeiling() {
            return recordedStageCeiling;
        }

        public int activeAllowedStage() {
            return activeAllowedStage;
        }

        public boolean hasActiveIntent() {
            return hasActiveIntent;
        }

        public String intentId() {
            return intentId;
        }

        public String scenarioId() {
            return scenarioId;
        }

        public String scenarioVersion() {
            return scenarioVersion;
        }

        public OptionalLong activeExpiryEpochSeconds() {
            return activeExpiryEpochSeconds;
        }

        public List<String> constraints() {
            return constraints;
        }

        public List<String> contextNotes() {
            return contextNotes;
        }

        public String lastConsumedIntentId() {
            return lastConsumedIntentId;
        }

        public long lastConsumedEpochSeconds() {
            return lastConsumedEpochSeconds;
        }

        public long generatedAtEpochSeconds() {
            return generatedAtEpochSeconds;
        }
    }
}
