package be.onboarding.processor;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

/**
 * Consumes ApplicationSubmitted events from Service Bus.
 *
 * <ul>
 *   <li>{@code %SERVICEBUS_QUEUE%} is resolved from the app setting (Terraform / local.settings.json).
 *   <li>{@code connection = "ServiceBusConnection"} points to the {@code ServiceBusConnection__*} app
 *       settings: identity-based, no connection string.
 *   <li>Returning normally completes the message. Throwing abandons it: Service Bus redelivers it, and
 *       after {@code max_delivery_count} (5) attempts moves it to the dead-letter queue.
 * </ul>
 */
public class ApplicationSubmittedFunction {

    private final ApplicationSubmittedHandler handler;

    public ApplicationSubmittedFunction() {
        this(new ApplicationSubmittedHandler());
    }

    ApplicationSubmittedFunction(ApplicationSubmittedHandler handler) {
        this.handler = handler;
    }

    @FunctionName("applicationSubmitted")
    public void run(
            @ServiceBusQueueTrigger(
                            name = "message",
                            queueName = "%SERVICEBUS_QUEUE%",
                            connection = "ServiceBusConnection")
                    String message,
            final ExecutionContext context) {
        try {
            ApplicationSubmitted event = handler.handle(message);
            // Only the id: names and e-mail addresses are personal data and stay out of the logs.
            context.getLogger().info(() -> "Processed application " + event.applicationId()
                    + " submitted at " + event.submittedAt());
        } catch (InvalidMessageException e) {
            context.getLogger().warning(() -> "Rejected message: " + e.getMessage());
            throw e;
        }
    }
}
