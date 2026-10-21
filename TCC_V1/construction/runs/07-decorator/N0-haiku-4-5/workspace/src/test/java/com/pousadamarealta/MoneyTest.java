package com.pousadamarealta;

import com.pousadamarealta.util.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoneyTest {
    @Test
    public void testRounding() {
        assertEquals(249.90, Money.round(249.90));
        assertEquals(299.88, Money.round(249.90 * 1.2));
    }

    @Test
    public void testNonRefundableDiscount() {
        double subtotal = 599.76;
        double discount = Money.applyPercentageDiscount(subtotal, 15);
        assertEquals(89.96, discount);
        assertEquals(509.80, Money.subtract(subtotal, discount));
    }
}
