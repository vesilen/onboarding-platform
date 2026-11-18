package be.onboarding.processor;

/** The message body is not a valid ApplicationSubmitted event. Retrying will not help. */
public class InvalidMessageException extends RuntimeException {

    public InvalidMessageException(String message) {
        super(message);
    }

    public InvalidMessageException(String message, Throwable cause) {
        super(message, cause);
    }
}
