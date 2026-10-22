package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    private final EmailValidator validator = new EmailValidator();

    @Test
    void validEmailsAreAccepted() {
        assertTrue(validator.isValid("ana@gmail.com"));
        assertTrue(validator.isValid("user@example.com.br"));
        assertTrue(validator.isValid("test.user@domain.co.uk"));
    }

    @Test
    void invalidEmailsAreRejected() {
        assertFalse(validator.isValid("ana@gmail"));
        assertFalse(validator.isValid("ana.gmail.com"));
        assertFalse(validator.isValid("@example.com"));
        assertFalse(validator.isValid("user@"));
        assertFalse(validator.isValid("user@@example.com"));
        assertFalse(validator.isValid("userexample.com"));
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid(null));
    }
}
