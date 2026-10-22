package com.pousadamareaita;

import org.junit.Test;
import static org.junit.Assert.*;
import com.pousadamareaita.util.MoneyUtils;
import java.math.BigDecimal;

public class MoneyUtilsTest {
    @Test
    public void testRoundHalfEven() {
        // Test rounding to nearest even (banker's rounding)
        assertEquals(new BigDecimal("2.50"), MoneyUtils.round(new BigDecimal("2.500")));
        assertEquals(new BigDecimal("2.50"), MoneyUtils.round(new BigDecimal("2.504")));
        assertEquals(new BigDecimal("2.51"), MoneyUtils.round(new BigDecimal("2.506")));
    }

    @Test
    public void testFormatBRL() {
        String formatted = MoneyUtils.formatBRL(new BigDecimal("1234.56"));
        assertEquals("R$ 1.234,56", formatted);
    }

    @Test
    public void testCalculationExample() {
        // Example: 10/07/2027 a 12/07/2027 (2 nights), STANDARD (249.90), high season
        // Night 1 (10/07): 249.90 * 1.20 = 299.88
        // Night 2 (11/07): 249.90 * 1.20 = 299.88
        // Total: 599.76
        BigDecimal rate = new BigDecimal("249.90");
        BigDecimal highSeasonRate = rate.multiply(new BigDecimal("1.20"));
        highSeasonRate = MoneyUtils.round(highSeasonRate);

        BigDecimal total = highSeasonRate.add(highSeasonRate);
        total = MoneyUtils.round(total);

        assertEquals(new BigDecimal("299.88"), highSeasonRate);
        assertEquals(new BigDecimal("599.76"), total);
    }
}
