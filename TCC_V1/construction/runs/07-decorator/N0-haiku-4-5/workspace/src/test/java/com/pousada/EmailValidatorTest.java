package com.pousada;

import com.pousada.utils.EmailValidator;
import org.junit.Test;
import static org.junit.Assert.*;

public class EmailValidatorTest {
    @Test
    public void validEmailAddresses() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
        assertTrue(EmailValidator.isValid("user@example.co.uk"));
        assertTrue(EmailValidator.isValid("test.email@domain.org"));
    }

    @Test
    public void invalidEmailsWithoutAt() {
        assertFalse(EmailValidator.isValid("anagmail.com"));
    }

    @Test
    public void invalidEmailsWithMultipleAt() {
        assertFalse(EmailValidator.isValid("ana@gmail@com"));
    }

    @Test
    public void invalidEmailsWithoutDomainDot() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    public void invalidEmailsWithoutLocalPart() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    public void invalidEmailsWithoutDomain() {
        assertFalse(EmailValidator.isValid("ana@"));
    }

    @Test
    public void invalidEmptyEmail() {
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid(null));
    }

    @Test
    public void invalidEmailsWithDotAtEnd() {
        assertFalse(EmailValidator.isValid("ana@gmail."));
    }
}
