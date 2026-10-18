package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    @Test
    void validEmails() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("joao.silva@empresa.com.br"));
        assertTrue(EmailValidator.isValid("test@example.co.uk"));
        assertTrue(EmailValidator.isValid("user+tag@domain.com"));
    }

    @Test
    void invalidEmailsWithoutAt() {
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
        assertFalse(EmailValidator.isValid("anagmail.com"));
    }

    @Test
    void invalidEmailsWithMultipleAt() {
        assertFalse(EmailValidator.isValid("ana@@gmail.com"));
        assertFalse(EmailValidator.isValid("ana@test@gmail.com"));
    }

    @Test
    void invalidEmailsWithoutDomainDot() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
        assertFalse(EmailValidator.isValid("test@localhost"));
    }

    @Test
    void invalidEmailsWithoutLocalPart() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    void invalidEmailsNullOrBlank() {
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid("   "));
    }
}
