package com.loja;

import com.loja.util.MoneyUtils;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyUtilsTest {
    @Test
    void testHalfEvenRounding() {
        assertEquals(new BigDecimal("3.00"), MoneyUtils.roundMoney(new BigDecimal("2.995")));
        assertEquals(new BigDecimal("2.98"), MoneyUtils.roundMoney(new BigDecimal("2.985")));
        assertEquals(new BigDecimal("2.99"), MoneyUtils.roundMoney(new BigDecimal("2.99")));
        assertEquals(new BigDecimal("2.99"), MoneyUtils.roundMoney(new BigDecimal("2.9949999")));
    }
}
