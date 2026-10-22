package hotel.messages;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestModeEmailSenderTest {

    @Test
    void testModeRuleRedirectsEmail() {
        EmailToSend email = new EmailToSend("guest@example.com", "Confirmation", "Body");
        TestModeRule rule = new TestModeRule();
        ProcessingResult result = rule.process(email);

        assertFalse(result.isBlocked());
        EmailToSend modifiedEmail = result.email();
        assertEquals("guest@example.com", modifiedEmail.to());
    }

    @Test
    void validateEmailRuleBlocksInvalidEmail() {
        EmailToSend email = new EmailToSend("invalid-email", "Subject", "Body");
        ValidateEmailRule rule = new ValidateEmailRule();
        ProcessingResult result = rule.process(email);

        assertTrue(result.isBlocked());
    }

    @Test
    void validateEmailRuleAllowsValidEmail() {
        EmailToSend email = new EmailToSend("guest@example.com", "Subject", "Body");
        ValidateEmailRule rule = new ValidateEmailRule();
        ProcessingResult result = rule.process(email);

        assertFalse(result.isBlocked());
        assertEquals("guest@example.com", result.email().to());
    }
}
