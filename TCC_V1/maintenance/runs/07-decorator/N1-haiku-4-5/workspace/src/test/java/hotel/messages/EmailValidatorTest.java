package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    @Test
    void acceptsValidEmails() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("user@example.co.uk"));
        assertTrue(EmailValidator.isValid("a@b.c"));
        assertTrue(EmailValidator.isValid("carla@example.com"));
    }

    @Test
    void rejectsEmailsWithoutAt() {
        assertFalse(EmailValidator.isValid("anagmail.com"));
        assertFalse(EmailValidator.isValid("gmail.com"));
    }

    @Test
    void rejectsEmailsWithMultipleAts() {
        assertFalse(EmailValidator.isValid("ana@gmail@com"));
    }

    @Test
    void rejectsEmailsWithoutLocalPart() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    void rejectsEmailsWithoutDomain() {
        assertFalse(EmailValidator.isValid("ana@"));
    }

    @Test
    void rejectsDomainsWithoutDot() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    void rejectsNullAndBlank() {
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid("   "));
    }

    @Test
    void rejectsDomainsStartingWithDot() {
        assertFalse(EmailValidator.isValid("ana@.gmail.com"));
    }

    @Test
    void rejectsDomainsEndingWithDot() {
        assertFalse(EmailValidator.isValid("ana@gmail.com."));
    }
}
