package com.pousada.marealta.util;

import junit.framework.TestCase;

public class MoneyRounderTest extends TestCase {
    public void testRound() {
        // Test HALF_EVEN rounding (meios para pares)
        assertEquals(2.50, MoneyRounder.round(2.5));
        assertEquals(3.50, MoneyRounder.round(3.5));
        assertEquals(2.46, MoneyRounder.round(2.455));
        assertEquals(2.44, MoneyRounder.round(2.445));
    }
}
