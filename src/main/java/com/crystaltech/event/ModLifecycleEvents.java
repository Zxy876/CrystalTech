package com.crystaltech.event;

import java.util.List;

import com.crystaltech.capability.CrystalStageCapability;
import com.crystaltech.capability.ICrystalStage;
import com.crystaltech.capability.intent.IPlayerIntent;
import com.crystaltech.capability.intent.PlayerIntentCapability;
import com.crystaltech.core.crafting.StageUnlockedCondition;
import com.crystaltech.network.CrystalTechNetwork;
import com.crystaltech.protocol.FileManifestationIntentInbox;
import com.crystaltech.protocol.IntentSignatureValidatorFactory;
import com.crystaltech.protocol.ManifestationEventWriter;
import com.crystaltech.protocol.ManifestationIntentService;
import com.crystaltech.protocol.ProtocolFileLayout;
import com.crystaltech.protocol.ProtocolOutputs;
import com.crystaltech.protocol.ProtocolTelemetryService;
import com.crystaltech.protocol.SocialFeedWriter;
import com.crystaltech.protocol.TechnologyStatusWriter;

import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Registers listeners bound to the mod event bus. Forge invokes these during
 * the mod loading lifecycle.
 */
public final class ModLifecycleEvents {
    private ModLifecycleEvents() {
    }

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModLifecycleEvents::onRegisterCapabilities);
        modEventBus.addListener(ModLifecycleEvents::onCommonSetup);
    }

    public static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ICrystalStage.class);
        CrystalStageCapability.logRegistered();
        event.register(IPlayerIntent.class);
        PlayerIntentCapability.logRegistered();
    }

    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CrystalTechNetwork.register();
            CraftingHelper.register(StageUnlockedCondition.Serializer.INSTANCE);
            ProtocolFileLayout layout = ProtocolFileLayout.resolve(FMLPaths.GAMEDIR.get());
            ManifestationIntentService.getInstance().configure(new ManifestationIntentService.Configuration(
                    new FileManifestationIntentInbox(layout.inboxRoot()),
                    IntentSignatureValidatorFactory.fromEnvironment(),
                    ManifestationEventWriter.forFile(layout.eventLogFile()),
                    List.of("0.1.0")));
            SocialFeedWriter feedWriter = SocialFeedWriter.forDirectory(layout.socialFeedDir());
            TechnologyStatusWriter statusWriter = TechnologyStatusWriter.forFile(layout.technologyStatusFile());
            ProtocolOutputs.install(feedWriter, statusWriter);
            ProtocolTelemetryService.getInstance().configure(statusWriter, layout.inboxRoot());
        });
    }
}
