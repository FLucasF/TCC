package com.pousada.marealta.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailValidatorTest {
    @Test
    void shouldValidateCorrectEmail() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("user.name@example.co.uk"));
    }

    @Test
    void shouldRejectEmailWithoutAt() {
        assertFalse(EmailValidator.isValid("anagmail.com"));
    }

    @Test
    void shouldRejectEmailWithMultipleAt() {
        assertFalse(EmailValidator.isValid("ana@gm@il.com"));
    }

    @Test
    void shouldRejectEmailWithoutDomain() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    void shouldRejectEmailWithoutLocalPart() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    void shouldRejectEmailWithoutDot() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    void shouldRejectNullEmail() {
        assertFalse(EmailValidator.isValid(null));
    }

    @Test
    void shouldRejectBlankEmail() {
        assertFalse(EmailValidator.isValid(""));
    }
}
