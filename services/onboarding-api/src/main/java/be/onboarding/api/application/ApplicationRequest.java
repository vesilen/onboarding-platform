package be.onboarding.api.application;

/** What a channel (web, mobile, partner) sends to start an onboarding application. */
public record ApplicationRequest(String firstName, String lastName, String email) {}
