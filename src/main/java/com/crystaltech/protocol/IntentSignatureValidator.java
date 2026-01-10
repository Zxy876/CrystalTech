package com.crystaltech.protocol;

import java.util.Objects;

/**
 * Validates the cryptographic signature supplied with a manifestation intent.
 */
public interface IntentSignatureValidator {

    ValidationResult validate(ManifestationIntent intent, String rawPayload);

    static IntentSignatureValidator permissive(String status) {
        return (intent, rawPayload) -> ValidationResult.success(status == null ? "skipped" : status);
    }

    record ValidationResult(boolean valid, String status) {
        public static ValidationResult success(String status) {
            return new ValidationResult(true, Objects.requireNonNullElse(status, "verified"));
        }

        public static ValidationResult failure(String status) {
            return new ValidationResult(false, Objects.requireNonNullElse(status, "invalid"));
        }
    }
}
