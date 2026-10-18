package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    @Test
    void acceptsValidEmails() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("carla@example.com"));
        assertTrue(EmailValidator.isValid("test@pousadamarealta.com.br"));
    }

    @Test
    void rejectsEmailsWithoutAtSign() {
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
        assertFalse(EmailValidator.isValid("carlaexample.com"));
    }

    @Test
    void rejectsEmailsWithMultipleAtSigns() {
        assertFalse(EmailValidator.isValid("ana@@gmail.com"));
        assertFalse(EmailValidator.isValid("ana@test@example.com"));
    }

    @Test
    void rejectsEmailsWithoutDomainDot() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
        assertFalse(EmailValidator.isValid("carla@example"));
    }

    @Test
    void rejectsEmailsWithoutLocalPart() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    void rejectsNullAndBlankEmails() {
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid("   "));
    }
}
