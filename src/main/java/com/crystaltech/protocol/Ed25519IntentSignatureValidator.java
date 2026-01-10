package com.crystaltech.protocol;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import com.crystaltech.CrystalTech;

/**
 * Ed25519-based signature validator for manifestation intents.
 */
public final class Ed25519IntentSignatureValidator implements IntentSignatureValidator {
    private static final String SIGNATURE_ALGORITHM = "Ed25519";

    private final PublicKey publicKey;

    private Ed25519IntentSignatureValidator(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public static Ed25519IntentSignatureValidator fromEncodedKey(byte[] encodedKey) {
        try {
            KeyFactory factory = KeyFactory.getInstance(SIGNATURE_ALGORITHM);
            PublicKey key = factory.generatePublic(new X509EncodedKeySpec(encodedKey));
            return new Ed25519IntentSignatureValidator(key);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to parse Ed25519 public key", ex);
        }
    }

    @Override
    public ValidationResult validate(ManifestationIntent intent, String rawPayload) {
        if (rawPayload == null) {
            return ValidationResult.failure("missing_raw_payload");
        }
        if (intent.signature() == null || intent.signature().isBlank()) {
            return ValidationResult.failure("signature_missing");
        }
        byte[] signatureBytes;
        try {
            signatureBytes = Base64.getDecoder().decode(intent.signature());
        } catch (IllegalArgumentException ex) {
            return ValidationResult.failure("signature_base64_invalid");
        }

        try {
            Signature verifier = Signature.getInstance(SIGNATURE_ALGORITHM);
            verifier.initVerify(publicKey);
            verifier.update(rawPayload.getBytes(StandardCharsets.UTF_8));
            boolean matches = verifier.verify(signatureBytes);
            if (!matches) {
                return ValidationResult.failure("signature_mismatch");
            }
            return ValidationResult.success("verified");
        } catch (GeneralSecurityException ex) {
            CrystalTech.LOGGER.error("Failed to verify manifestation intent signature for {}", intent.intentId(), ex);
            return ValidationResult.failure("signature_error");
        }
    }
}
