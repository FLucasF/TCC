package hotel.messages;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EmailValidatorTest {

    private final EmailValidator validator = new EmailValidator();

    @Test
    void validEmailsAreAccepted() {
        assertTrue(validator.isValid("ana@gmail.com"));
        assertTrue(validator.isValid("teste@pousadamarealta.com.br"));
        assertTrue(validator.isValid("user@example.org"));
        assertTrue(validator.isValid("a@b.c"));
    }

    @Test
    void invalidEmailsAreRejected() {
        assertFalse(validator.isValid("ana@gmail"));
        assertFalse(validator.isValid("ana.gmail.com"));
        assertFalse(validator.isValid("@gmail.com"));
        assertFalse(validator.isValid("ana@"));
        assertFalse(validator.isValid("ana@@gmail.com"));
        assertFalse(validator.isValid(""));
        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid("   "));
    }
}
