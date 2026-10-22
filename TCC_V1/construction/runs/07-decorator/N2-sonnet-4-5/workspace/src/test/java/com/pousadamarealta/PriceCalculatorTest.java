package com.pousadamarealta;

import com.pousadamarealta.domain.PriceCalculator;
import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorTest {
    private final PriceCalculator calculator = new PriceCalculator();

    @Test
    void testRegularSeasonPricing() {
        BigDecimal subtotal = calculator.calculateSubtotal(
            RoomType.STANDARD,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12)
        );
        assertEquals(new BigDecimal("499.80"), subtotal);
    }

    @Test
    void testHighSeasonPricing() {
        BigDecimal subtotal = calculator.calculateSubtotal(
            RoomType.STANDARD,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12)
        );
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("499.80");
        BigDecimal discount = calculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("74.97"), discount);
    }

    @Test
    void testStandardPlanNoDiscount() {
        BigDecimal subtotal = new BigDecimal("499.80");
        BigDecimal discount = calculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(0, discount.compareTo(BigDecimal.ZERO));
    }

    @Test
    void testTotalCalculation() {
        BigDecimal subtotal = new BigDecimal("499.80");
        BigDecimal discount = new BigDecimal("74.97");
        BigDecimal total = calculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("424.83"), total);
    }
}
