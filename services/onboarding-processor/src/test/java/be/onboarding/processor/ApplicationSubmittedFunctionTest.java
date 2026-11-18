package be.onboarding.processor;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.azure.functions.ExecutionContext;
import java.util.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationSubmittedFunctionTest {

    private final ApplicationSubmittedFunction function = new ApplicationSubmittedFunction();
    private final ExecutionContext context = mock(ExecutionContext.class);

    @BeforeEach
    void setUp() {
        when(context.getLogger()).thenReturn(Logger.getLogger("test"));
    }

    @Test
    void completesAValidMessage() {
        assertThatCode(() -> function.run(
                        "{\"applicationId\":\"00000000-0000-0000-0000-000000000001\"}", context))
                .doesNotThrowAnyException();
    }

    @Test
    void throwsOnAnInvalidMessageSoServiceBusRetriesAndDeadLetters() {
        assertThatThrownBy(() -> function.run("not json", context))
                .isInstanceOf(InvalidMessageException.class);
    }
}
