package be.onboarding.api.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published on the {@code application-submitted} queue when a channel submits an application.
 *
 * <p>Contract with onboarding-processor, which has its own copy of this record. Adding fields is
 * safe (the processor ignores unknown fields); renaming or removing them is a breaking change.
 */
public record ApplicationSubmitted(
        UUID applicationId, String firstName, String lastName, String email, Instant submittedAt) {}
