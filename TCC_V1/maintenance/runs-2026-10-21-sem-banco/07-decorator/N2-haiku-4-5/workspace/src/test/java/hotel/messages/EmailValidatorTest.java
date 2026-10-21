package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    @Test
    void acceptsValidEmails() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("user.name@example.co.uk"));
        assertTrue(EmailValidator.isValid("a@b.c"));
    }

    @Test
    void rejectsInvalidEmails() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
        assertFalse(EmailValidator.isValid("ana@@gmail.com"));
        assertFalse(EmailValidator.isValid("@example.com"));
        assertFalse(EmailValidator.isValid("user@"));
        assertFalse(EmailValidator.isValid("user@nodot"));
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid("   "));
    }
}
