package com.pousada.marealta.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorTest {
    @Test
    void testValidEmail() {
        assertTrue(EmailValidator.isValid("user@example.com"));
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
    }

    @Test
    void testInvalidEmailNoAt() {
        assertFalse(EmailValidator.isValid("usergmail.com"));
    }

    @Test
    void testInvalidEmailMultipleAt() {
        assertFalse(EmailValidator.isValid("user@@example.com"));
    }

    @Test
    void testInvalidEmailNoLocalPart() {
        assertFalse(EmailValidator.isValid("@example.com"));
    }

    @Test
    void testInvalidEmailNoDot() {
        assertFalse(EmailValidator.isValid("user@examplecom"));
    }

    @Test
    void testInvalidEmailDotAtEnd() {
        assertFalse(EmailValidator.isValid("user@example."));
    }

    @Test
    void testMissingDomain() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }
}
