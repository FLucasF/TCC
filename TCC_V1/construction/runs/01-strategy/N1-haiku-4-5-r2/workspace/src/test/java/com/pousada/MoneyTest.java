package com.pousada;

import com.pousada.domain.Money;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

public class MoneyTest {

    @Test
    public void testRounding() {
        BigDecimal value = new BigDecimal("249.90").multiply(new BigDecimal("1.20"));
        BigDecimal rounded = Money.round(value);
        assertEquals(new BigDecimal("299.88"), rounded);
    }

    @Test
    public void testPercentOf() {
        BigDecimal value = new BigDecimal("779.80");
        BigDecimal discount = Money.percentOf(value, 12);
        assertEquals(new BigDecimal("93.58"), discount);
    }

    @Test
    public void testLongStayDiscount() {
        BigDecimal subtotal = new BigDecimal("1749.30");
        BigDecimal discount = Money.percentOf(subtotal, 10);
        assertEquals(new BigDecimal("174.93"), discount);
    }
}
