package be.onboarding.processor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published by onboarding-api on the {@code application-submitted} queue after a POST.
 *
 * <p>JSON contract (unknown fields are ignored, so the producer can add fields safely):
 * <pre>{@code
 * {"applicationId":"…uuid…","firstName":"Ada","lastName":"Lovelace",
 *  "email":"ada@example.com","submittedAt":"2026-11-17T10:15:30Z"}
 * }</pre>
 */
public record ApplicationSubmitted(
        UUID applicationId, String firstName, String lastName, String email, Instant submittedAt) {}
