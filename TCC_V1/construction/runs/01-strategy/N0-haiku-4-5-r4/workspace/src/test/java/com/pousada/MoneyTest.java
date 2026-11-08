package com.pousada;

import com.pousada.util.Money;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoneyTest {
    @Test
    public void testRounding() {
        BigDecimal value = new BigDecimal("249.90");
        BigDecimal multiplied = Money.multiply(value, BigDecimal.valueOf(1.2));
        assertEquals(new BigDecimal("299.88"), multiplied);
    }

    @Test
    public void testDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = Money.percent(subtotal, 12);
        assertEquals(new BigDecimal("93.58"), discount);
    }

    @Test
    public void testAdd() {
        BigDecimal a = new BigDecimal("299.88");
        BigDecimal b = new BigDecimal("299.88");
        BigDecimal sum = Money.add(a, b);
        assertEquals(new BigDecimal("599.76"), sum);
    }
}
