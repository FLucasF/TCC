package com.pousada.marealta.util;

import junit.framework.TestCase;

public class EmailValidatorTest extends TestCase {
    public void testValidEmail() {
        assertTrue(EmailValidator.isValid("user@example.com"));
        assertTrue(EmailValidator.isValid("john@gmail.com"));
        assertTrue(EmailValidator.isValid("a@b.c"));
    }

    public void testInvalidEmail() {
        assertFalse(EmailValidator.isValid("user@"));
        assertFalse(EmailValidator.isValid("@example.com"));
        assertFalse(EmailValidator.isValid("userexample.com"));
        assertFalse(EmailValidator.isValid("user@domain"));
        assertFalse(EmailValidator.isValid("user@@example.com"));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid(null));
    }
}
