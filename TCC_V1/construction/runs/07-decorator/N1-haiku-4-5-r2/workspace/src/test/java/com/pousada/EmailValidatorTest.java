package com.pousada;

import com.pousada.utils.EmailValidator;
import org.junit.Test;
import static org.junit.Assert.*;

public class EmailValidatorTest {
    @Test
    public void testValidEmails() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("user.name@domain.co.uk"));
        assertTrue(EmailValidator.isValid("test+tag@example.com"));
        assertTrue(EmailValidator.isValid("a@b.co"));
    }

    @Test
    public void testInvalidEmails() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
        assertFalse(EmailValidator.isValid("@gmail.com"));
        assertFalse(EmailValidator.isValid("ana@"));
        assertFalse(EmailValidator.isValid("ana@@gmail.com"));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid("ana@.com"));
        assertFalse(EmailValidator.isValid("ana@com."));
    }
}
