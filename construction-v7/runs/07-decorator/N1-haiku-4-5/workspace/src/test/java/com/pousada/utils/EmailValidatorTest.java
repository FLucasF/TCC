package com.pousada.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class EmailValidatorTest {
    @Test
    public void testValidEmail() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("user@example.co.uk"));
        assertTrue(EmailValidator.isValid("test@domain.org"));
    }

    @Test
    public void testInvalidEmailNoAt() {
        assertFalse(EmailValidator.isValid("anagmail.com"));
    }

    @Test
    public void testInvalidEmailMultipleAt() {
        assertFalse(EmailValidator.isValid("ana@@gmail.com"));
    }

    @Test
    public void testInvalidEmailNoDot() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    public void testInvalidEmailEmptyDomain() {
        assertFalse(EmailValidator.isValid("ana@"));
    }

    @Test
    public void testInvalidEmailAtStart() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    public void testInvalidEmailDotAtStart() {
        assertFalse(EmailValidator.isValid("ana@.gmail.com"));
    }

    @Test
    public void testInvalidEmailDotAtEnd() {
        assertFalse(EmailValidator.isValid("ana@gmail.com."));
    }

    @Test
    public void testEmptyEmail() {
        assertFalse(EmailValidator.isValid(""));
    }

    @Test
    public void testNullEmail() {
        assertFalse(EmailValidator.isValid(null));
    }
}
