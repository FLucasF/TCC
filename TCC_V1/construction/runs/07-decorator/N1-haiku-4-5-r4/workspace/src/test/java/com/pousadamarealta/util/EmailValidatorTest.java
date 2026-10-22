package com.pousadamarealta.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class EmailValidatorTest {
    @Test
    public void testValidEmail() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("user@example.co.uk"));
        assertTrue(EmailValidator.isValid("test.email@domain.org"));
    }

    @Test
    public void testInvalidEmails() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
        assertFalse(EmailValidator.isValid("@gmail.com"));
        assertFalse(EmailValidator.isValid("ana@@gmail.com"));
        assertFalse(EmailValidator.isValid("ana@.com"));
        assertFalse(EmailValidator.isValid("ana@gmail"));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid(null));
    }
}
