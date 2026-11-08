package com.pousada;

import com.pousada.util.Money;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class MoneyTest {
    @Test
    public void testRounding() {
        // Test banker's rounding
        BigDecimal value = new BigDecimal("1.125");
        BigDecimal rounded = Money.round(value);
        assertEquals(new BigDecimal("1.12"), rounded);

        BigDecimal value2 = new BigDecimal("1.135");
        BigDecimal rounded2 = Money.round(value2);
        assertEquals(new BigDecimal("1.14"), rounded2);
    }
}
