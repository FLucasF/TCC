package com.pousadamarealta.service;

import org.junit.Test;

import static org.junit.Assert.*;

public class EmailValidatorTest {

    @Test
    public void testValidEmail() {
        assertTrue(EmailValidator.isValid("ana@gmail.com"));
    }

    @Test
    public void testValidEmailMultipleDots() {
        assertTrue(EmailValidator.isValid("john.doe@example.co.uk"));
    }

    @Test
    public void testMissingAt() {
        assertFalse(EmailValidator.isValid("anagmail.com"));
    }

    @Test
    public void testMissingDot() {
        assertFalse(EmailValidator.isValid("ana@gmail"));
    }

    @Test
    public void testIncompleteEmail1() {
        assertFalse(EmailValidator.isValid("ana@.com"));
    }

    @Test
    public void testIncompleteEmail2() {
        assertFalse(EmailValidator.isValid("@gmail.com"));
    }

    @Test
    public void testMultipleAt() {
        assertFalse(EmailValidator.isValid("ana@@gmail.com"));
    }

    @Test
    public void testNull() {
        assertFalse(EmailValidator.isValid(null));
    }

    @Test
    public void testEmpty() {
        assertFalse(EmailValidator.isValid(""));
    }

    @Test
    public void testStartsWithDot() {
        assertFalse(EmailValidator.isValid("ana@.gmail.com"));
    }

    @Test
    public void testEndsWithDot() {
        assertFalse(EmailValidator.isValid("ana@gmail."));
    }

    @Test
    public void testValidComplexEmail() {
        assertTrue(EmailValidator.isValid("user.name+tag@example.co.uk"));
    }
}
