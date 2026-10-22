package com.pousada.marealta;

import com.pousada.marealta.util.EmailValidator;
import org.junit.Test;

import static org.junit.Assert.*;

public class EmailValidatorTest {

    @Test
    public void testValidEmail() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("test@example.co.uk"));
        assertTrue(EmailValidator.isValid("user.name@domain.org"));
    }

    @Test
    public void testInvalidEmailMissingAt() {
        assertFalse(EmailValidator.isValid("anagmail.com"));
    }

    @Test
    public void testInvalidEmailMultipleAt() {
        assertFalse(EmailValidator.isValid("ana@gmail@com"));
    }

    @Test
    public void testInvalidEmailMissingLocalPart() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    public void testInvalidEmailMissingDomain() {
        assertFalse(EmailValidator.isValid("ana@"));
    }

    @Test
    public void testInvalidEmailNoDotInDomain() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    public void testInvalidEmailDomainStartsWithDot() {
        assertFalse(EmailValidator.isValid("ana@.gmail.com"));
    }

    @Test
    public void testInvalidEmailDomainEndsWithDot() {
        assertFalse(EmailValidator.isValid("ana@gmail.com."));
    }

    @Test
    public void testInvalidEmailNull() {
        assertFalse(EmailValidator.isValid(null));
    }

    @Test
    public void testInvalidEmailEmpty() {
        assertFalse(EmailValidator.isValid(""));
    }

    @Test
    public void testTypo() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
        assertFalse(EmailValidator.isValid("ana.gmail.com"));
    }
}
