package com.pousada.marealta.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class EmailValidatorTest {
    @Test
    public void testValidEmail() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("user@example.co.uk"));
        assertTrue(EmailValidator.isValid("test.user@domain.org"));
    }

    @Test
    public void testInvalidEmailNoAt() {
        assertFalse(EmailValidator.isValid("anagmail.com"));
    }

    @Test
    public void testInvalidEmailMultipleAt() {
        assertFalse(EmailValidator.isValid("ana@gmail@com"));
    }

    @Test
    public void testInvalidEmailNoDomain() {
        assertFalse(EmailValidator.isValid("ana@"));
    }

    @Test
    public void testInvalidEmailNoDot() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    public void testInvalidEmailNoPrefix() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    public void testInvalidEmailNull() {
        assertFalse(EmailValidator.isValid(null));
    }

    @Test
    public void testInvalidEmailEmpty() {
        assertFalse(EmailValidator.isValid(""));
    }
}
