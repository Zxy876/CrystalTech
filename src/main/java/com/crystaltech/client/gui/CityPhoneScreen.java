package com.crystaltech.client.gui;

import com.crystaltech.network.CityPhoneDataMessage.CityPhoneSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Presents the player's current manifestation intent in a lightweight UI.
 */
public final class CityPhoneScreen extends Screen {
    private static final int PADDING = 16;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z")
            .withZone(ZoneId.systemDefault());

    private final CityPhoneSnapshot snapshot;

    public CityPhoneScreen(CityPhoneSnapshot snapshot) {
        super(Component.literal("CityPhone"));
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        int buttonWidth = 110;
        int buttonHeight = 20;
        int x = this.width / 2 - buttonWidth / 2;
        int y = this.height - PADDING - buttonHeight;
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(x, y, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        int contentWidth = this.width - PADDING * 2;
        int y = PADDING;
        guiGraphics.drawCenteredString(this.font, this.title, this.width / 2, y, 0xFFFFFF);
        y += this.font.lineHeight + 8;

        List<Component> lines = buildContent();
        for (Component line : lines) {
            for (FormattedCharSequence sequence : this.font.split(line, contentWidth)) {
                guiGraphics.drawString(this.font, sequence, PADDING, y, 0xE0E0E0, false);
                y += this.font.lineHeight + 2;
            }
            y += 4;
        }
    }

    private List<Component> buildContent() {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal("Current stage: " + snapshot.currentStage()));
        lines.add(Component.literal("Highest recorded stage: " + snapshot.recordedStageCeiling()));

        if (snapshot.hasActiveIntent()) {
            lines.add(Component.literal("Allowed stage cap: " + snapshot.activeAllowedStage()));
            if (snapshot.intentId() != null) {
                lines.add(Component.literal("Intent ID: " + snapshot.intentId()));
            }
            if (snapshot.scenarioId() != null) {
                String scenario = snapshot.scenarioId();
                if (snapshot.scenarioVersion() != null) {
                    scenario += " (" + snapshot.scenarioVersion() + ")";
                }
                lines.add(Component.literal("Scenario: " + scenario));
            }
            snapshot.activeExpiryEpochSeconds().ifPresent(expires ->
                    lines.add(Component.literal("Expires at: " + TIMESTAMP_FORMAT.format(Instant.ofEpochSecond(expires)))));
            if (snapshot.constraints().isEmpty()) {
                lines.add(Component.literal("Constraints: (none)"));
            } else {
                lines.add(Component.literal("Constraints:"));
                snapshot.constraints().forEach(constraint ->
                        lines.add(Component.literal(" - " + constraint)));
            }
            if (!snapshot.contextNotes().isEmpty()) {
                lines.add(Component.literal("Context notes:"));
                snapshot.contextNotes().forEach(note ->
                        lines.add(Component.literal(" - " + note)));
            }
        } else {
            lines.add(Component.literal("No active manifestation intent."));
        }

        if (snapshot.lastConsumedIntentId() != null) {
            lines.add(Component.literal("Last manifested intent: " + snapshot.lastConsumedIntentId()));
            if (snapshot.lastConsumedEpochSeconds() > 0) {
                lines.add(Component.literal("Completed at: " + TIMESTAMP_FORMAT.format(Instant.ofEpochSecond(snapshot.lastConsumedEpochSeconds()))));
            }
        }

        lines.add(Component.literal("Snapshot generated at: " + TIMESTAMP_FORMAT.format(Instant.ofEpochSecond(snapshot.generatedAtEpochSeconds()))));
        return lines;
    }
}
