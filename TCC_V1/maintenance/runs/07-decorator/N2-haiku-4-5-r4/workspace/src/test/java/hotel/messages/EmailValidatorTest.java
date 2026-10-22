package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    @Test
    void validatesCorrectEmail() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("carla@example.com"));
        assertTrue(EmailValidator.isValid("user.name@domain.co.uk"));
    }

    @Test
    void rejectsEmailWithoutAt() {
        assertFalse(EmailValidator.isValid("anagmail.com"));
    }

    @Test
    void rejectsEmailWithMultipleAt() {
        assertFalse(EmailValidator.isValid("ana@gmail@com"));
    }

    @Test
    void rejectsEmailWithoutLocalPart() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    void rejectsEmailWithoutDomain() {
        assertFalse(EmailValidator.isValid("ana@"));
    }

    @Test
    void rejectsEmailWithDomainWithoutDot() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    void rejectsBlankEmail() {
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid(null));
    }
}
