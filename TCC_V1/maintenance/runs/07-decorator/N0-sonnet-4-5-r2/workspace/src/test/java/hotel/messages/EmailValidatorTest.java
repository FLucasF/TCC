package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    @Test
    void validEmails() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("user@example.com.br"));
        assertTrue(EmailValidator.isValid("test.user@domain.co"));
        assertTrue(EmailValidator.isValid("a@b.c"));
    }

    @Test
    void invalidEmailsWithoutAt() {
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid("   "));
    }

    @Test
    void invalidEmailsWithMultipleAt() {
        assertFalse(EmailValidator.isValid("ana@@example.com"));
        assertFalse(EmailValidator.isValid("ana@test@example.com"));
    }

    @Test
    void invalidEmailsWithMissingLocalPart() {
        assertFalse(EmailValidator.isValid("@example.com"));
    }

    @Test
    void invalidEmailsWithoutDotInDomain() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
        assertFalse(EmailValidator.isValid("user@domain"));
    }

    @Test
    void invalidEmailsWithMissingDomain() {
        assertFalse(EmailValidator.isValid("ana@"));
    }

    @Test
    void nullAndBlankEmails() {
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid("   "));
    }
}
