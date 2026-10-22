package com.pousadamarealta.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class EmailValidatorTest {
    @Test
    public void testValidEmail() {
        assertTrue(EmailValidator.isValid("user@example.com"));
        assertTrue(EmailValidator.isValid("john.doe@company.co.uk"));
        assertTrue(EmailValidator.isValid("archive@pousadamarealta.com.br"));
    }

    @Test
    public void testInvalidEmailMissingAt() {
        assertFalse(EmailValidator.isValid("usergmail.com"));
    }

    @Test
    public void testInvalidEmailMultipleAt() {
        assertFalse(EmailValidator.isValid("user@example@com"));
    }

    @Test
    public void testInvalidEmailNoDomainDot() {
        assertFalse(EmailValidator.isValid("user@gmail"));
    }

    @Test
    public void testInvalidEmailAtStart() {
        assertFalse(EmailValidator.isValid("@example.com"));
    }

    @Test
    public void testInvalidEmailAtEnd() {
        assertFalse(EmailValidator.isValid("user@"));
    }

    @Test
    public void testInvalidEmailEmpty() {
        assertFalse(EmailValidator.isValid(""));
    }

    @Test
    public void testInvalidEmailNull() {
        assertFalse(EmailValidator.isValid(null));
    }

    @Test
    public void testInvalidEmailDotAtStart() {
        assertFalse(EmailValidator.isValid("user@.com"));
    }

    @Test
    public void testInvalidEmailDotAtEnd() {
        assertFalse(EmailValidator.isValid("user@gmail."));
    }
}
