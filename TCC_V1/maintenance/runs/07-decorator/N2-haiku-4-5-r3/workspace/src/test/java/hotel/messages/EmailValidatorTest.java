package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    @Test
    void validatesCorrectEmailAddresses() {
        assertTrue(EmailValidator.isValid("ana@example.com"));
        assertTrue(EmailValidator.isValid("user@domain.co.uk"));
        assertTrue(EmailValidator.isValid("a@b.c"));
    }

    @Test
    void rejectsEmailsWithoutAtSign() {
        assertFalse(EmailValidator.isValid("anaexample.com"));
    }

    @Test
    void rejectsEmailsWithMultipleAtSigns() {
        assertFalse(EmailValidator.isValid("ana@example@com"));
    }

    @Test
    void rejectsEmailsWithNothingBeforeAtSign() {
        assertFalse(EmailValidator.isValid("@example.com"));
    }

    @Test
    void rejectsEmailsWithNothingAfterAtSign() {
        assertFalse(EmailValidator.isValid("ana@"));
    }

    @Test
    void rejectsDomainsWithoutDot() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    void rejectsBlankEmails() {
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid(null));
    }
}
