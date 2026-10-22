package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    @Test
    void validatesProperEmails() {
        assertTrue(EmailValidator.isValid("ana@example.com"));
        assertTrue(EmailValidator.isValid("carla.dias@test.com.br"));
        assertTrue(EmailValidator.isValid("user+tag@domain.co.uk"));
    }

    @Test
    void rejectsInvalidEmails() {
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid("   "));
        assertFalse(EmailValidator.isValid("ana"));
        assertFalse(EmailValidator.isValid("ana@"));
        assertFalse(EmailValidator.isValid("ana@gmail"));
        assertFalse(EmailValidator.isValid("@gmail.com"));
        assertFalse(EmailValidator.isValid("ana@@gmail.com"));
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
    }
}
