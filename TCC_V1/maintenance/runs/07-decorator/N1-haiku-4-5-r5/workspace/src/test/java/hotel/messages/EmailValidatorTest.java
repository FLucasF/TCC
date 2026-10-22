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
        assertTrue(EmailValidator.isValid("carla@example.com"));
    }

    @Test
    void rejectsEmailsWithoutAt() {
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
    }

    @Test
    void rejectsEmailsWithMultipleAts() {
        assertFalse(EmailValidator.isValid("ana@@gmail.com"));
        assertFalse(EmailValidator.isValid("ana@gm@il.com"));
    }

    @Test
    void rejectsEmailsWithoutLocalPart() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    void rejectsEmailsWithoutDomainPart() {
        assertFalse(EmailValidator.isValid("ana@"));
    }

    @Test
    void rejectsEmailsWithoutDotInDomain() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    void rejectsEmailsWithDotAtStartOfDomain() {
        assertFalse(EmailValidator.isValid("ana@.gmail.com"));
    }

    @Test
    void rejectsEmailsWithDotAtEndOfDomain() {
        assertFalse(EmailValidator.isValid("ana@gmail.com."));
    }

    @Test
    void rejectsBlankEmails() {
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid(null));
    }
}
