package com.pousadamarealta.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationUtilTest {
    @Test
    public void testValidEmail() {
        assertTrue(ValidationUtil.isValidEmail("ana@gmail.com"));
        assertTrue(ValidationUtil.isValidEmail("user@example.co.uk"));
        assertTrue(ValidationUtil.isValidEmail("test@sub.domain.com"));
    }

    @Test
    public void testInvalidEmail() {
        assertFalse(ValidationUtil.isValidEmail("ana@gmail"));
        assertFalse(ValidationUtil.isValidEmail("ana.gmail.com"));
        assertFalse(ValidationUtil.isValidEmail("@gmail.com"));
        assertFalse(ValidationUtil.isValidEmail("ana@"));
        assertFalse(ValidationUtil.isValidEmail("ana@@gmail.com"));
        assertFalse(ValidationUtil.isValidEmail(""));
        assertFalse(ValidationUtil.isValidEmail(null));
    }

    @Test
    public void testValidDateRange() {
        var checkIn = java.time.LocalDate.of(2027, 7, 10);
        var checkOut = java.time.LocalDate.of(2027, 7, 12);
        assertTrue(ValidationUtil.isValidDateRange(checkIn, checkOut));
    }

    @Test
    public void testInvalidDateRange() {
        var checkIn = java.time.LocalDate.of(2027, 7, 12);
        var checkOut = java.time.LocalDate.of(2027, 7, 10);
        assertFalse(ValidationUtil.isValidDateRange(checkIn, checkOut));

        assertFalse(ValidationUtil.isValidDateRange(null, java.time.LocalDate.of(2027, 7, 10)));
    }
}
