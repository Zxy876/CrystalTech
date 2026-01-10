package com.crystaltech.protocol;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;

import com.crystaltech.CrystalTech;

/**
 * Builds signature validators from environment configuration.
 */
public final class IntentSignatureValidatorFactory {
    private static final String ENV_PUBLIC_KEY_FILE = "CRYSTALTECH_PROTOCOL_PUBLIC_KEY_FILE";
    private static final String ENV_PUBLIC_KEY_INLINE = "CRYSTALTECH_PROTOCOL_PUBLIC_KEY";
    private static final String ENV_ALLOW_UNSIGNED = "CRYSTALTECH_PROTOCOL_ALLOW_UNSIGNED";

    private IntentSignatureValidatorFactory() {
    }

    public static IntentSignatureValidator fromEnvironment() {
        if (isAllowUnsigned()) {
            CrystalTech.LOGGER.warn("Manifestation intent signature verification disabled via {}", ENV_ALLOW_UNSIGNED);
            return IntentSignatureValidator.permissive("unsigned_allowed");
        }

        byte[] key = locateKeyMaterial();
        if (key == null) {
            CrystalTech.LOGGER.warn("No public key configured for manifestation intents; verification will be skipped");
            return IntentSignatureValidator.permissive("unsigned_not_configured");
        }

        try {
            return Ed25519IntentSignatureValidator.fromEncodedKey(key);
        } catch (IllegalStateException ex) {
            CrystalTech.LOGGER.error("Failed to configure Ed25519 validator; falling back to permissive mode", ex);
            return IntentSignatureValidator.permissive("unsigned_invalid_key");
        }
    }

    private static boolean isAllowUnsigned() {
        String value = System.getenv(ENV_ALLOW_UNSIGNED);
        if (value == null) {
            return false;
        }
        String normalised = value.trim().toLowerCase(Locale.ROOT);
        return normalised.equals("1") || normalised.equals("true") || normalised.equals("yes");
    }

    private static byte[] locateKeyMaterial() {
        String filePath = System.getenv(ENV_PUBLIC_KEY_FILE);
        if (filePath != null && !filePath.isBlank()) {
            try {
                return stripPemHeaders(Files.readString(Path.of(filePath.trim())));
            } catch (IOException ex) {
                CrystalTech.LOGGER.error("Failed to read manifestation intent public key file {}", filePath, ex);
            }
        }
        String inline = System.getenv(ENV_PUBLIC_KEY_INLINE);
        if (inline != null && !inline.isBlank()) {
            return stripPemHeaders(inline);
        }
        return null;
    }

    private static byte[] stripPemHeaders(String raw) {
        String normalised = raw.replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        if (normalised.isEmpty()) {
            return null;
        }
        try {
            return Base64.getDecoder().decode(normalised);
        } catch (IllegalArgumentException ex) {
            CrystalTech.LOGGER.error("Public key material is not valid Base64", ex);
            return null;
        }
    }
}
