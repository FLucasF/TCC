package com.pousadamarealta;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.service.PricingCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @Test
    void testHighSeasonJuly() {
        var result = calculator.calculate(
            RoomType.STANDARD,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            RatePlan.STANDARD
        );

        assertEquals(2, result.getNights());
        assertEquals(new BigDecimal("599.76"), result.getSubtotal());
        assertEquals(0, result.getDiscount().compareTo(new BigDecimal("0.00")));
        assertEquals(new BigDecimal("599.76"), result.getTotal());
    }

    @Test
    void testHighSeasonDecemberToFebruary() {
        var result = calculator.calculate(
            RoomType.DELUXE,
            LocalDate.of(2027, 12, 20),
            LocalDate.of(2028, 1, 5),
            RatePlan.STANDARD
        );

        assertEquals(16, result.getNights());
    }

    @Test
    void testLowSeason() {
        var result = calculator.calculate(
            RoomType.DELUXE,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            RatePlan.STANDARD
        );

        assertEquals(2, result.getNights());
        assertEquals(new BigDecimal("779.80"), result.getSubtotal());
        assertEquals(0, result.getDiscount().compareTo(new BigDecimal("0.00")));
        assertEquals(new BigDecimal("779.80"), result.getTotal());
    }

    @Test
    void testNonRefundableDiscount() {
        var result = calculator.calculate(
            RoomType.DELUXE,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            RatePlan.NON_REFUNDABLE
        );

        assertEquals(2, result.getNights());
        assertEquals(new BigDecimal("779.80"), result.getSubtotal());
        assertEquals(new BigDecimal("116.97"), result.getDiscount());
        assertEquals(new BigDecimal("662.83"), result.getTotal());
    }

    @Test
    void testSingleNight() {
        var result = calculator.calculate(
            RoomType.SUITE,
            LocalDate.of(2027, 5, 10),
            LocalDate.of(2027, 5, 11),
            RatePlan.STANDARD
        );

        assertEquals(1, result.getNights());
        assertEquals(new BigDecimal("579.90"), result.getSubtotal());
    }
}
