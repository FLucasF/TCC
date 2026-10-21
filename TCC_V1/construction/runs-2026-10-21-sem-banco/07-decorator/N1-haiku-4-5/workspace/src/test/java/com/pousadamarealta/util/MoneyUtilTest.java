package com.pousadamarealta.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

public class MoneyUtilTest {
    @Test
    public void testBankersRounding() {
        BigDecimal value1 = new BigDecimal("10.125");
        assertEquals(new BigDecimal("10.12"), MoneyUtil.round(value1));

        BigDecimal value2 = new BigDecimal("10.135");
        assertEquals(new BigDecimal("10.14"), MoneyUtil.round(value2));

        BigDecimal value3 = new BigDecimal("10.145");
        assertEquals(new BigDecimal("10.14"), MoneyUtil.round(value3));

        BigDecimal value4 = new BigDecimal("10.155");
        assertEquals(new BigDecimal("10.16"), MoneyUtil.round(value4));
    }

    @Test
    public void testRoundingToTwoDecimals() {
        BigDecimal value = new BigDecimal("123.456");
        assertEquals(new BigDecimal("123.46"), MoneyUtil.round(value));
    }
}
