package com.pousadamarealta;

import static org.junit.jupiter.api.Assertions.*;

import com.pousadamarealta.util.EmailValidator;
import org.junit.jupiter.api.Test;

class EmailValidatorTest {
    @Test
    void testValidEmail() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("teste@example.org"));
        assertTrue(EmailValidator.isValid("user.name@domain.co.uk"));
    }

    @Test
    void testInvalidEmailNoAt() {
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
    }

    @Test
    void testInvalidEmailNoPoint() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    void testInvalidEmailMultipleAt() {
        assertFalse(EmailValidator.isValid("ana@gmai@l.com"));
    }

    @Test
    void testInvalidEmailEmptyBefore() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    void testInvalidEmailNull() {
        assertFalse(EmailValidator.isValid(null));
    }

    @Test
    void testInvalidEmailEmpty() {
        assertFalse(EmailValidator.isValid(""));
    }
}
