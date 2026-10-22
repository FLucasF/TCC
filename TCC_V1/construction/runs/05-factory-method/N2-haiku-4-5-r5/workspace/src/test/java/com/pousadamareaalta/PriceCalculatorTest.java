package com.pousadamareaalta;

import com.pousadamareaalta.utils.PriceCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PriceCalculatorTest {
    @Test
    public void testHighSeasonCalculation() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        double subtotal = PriceCalculator.calculateSubtotal(checkIn, checkOut, 249.90);
        assertEquals(599.76, subtotal, 0.01);
    }

    @Test
    public void testLowSeasonCalculation() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        double subtotal = PriceCalculator.calculateSubtotal(checkIn, checkOut, 249.90);
        assertEquals(499.80, subtotal, 0.01);
    }

    @Test
    public void testDecemberHighSeason() {
        LocalDate checkIn = LocalDate.of(2027, 12, 15);
        LocalDate checkOut = LocalDate.of(2027, 12, 17);

        double subtotal = PriceCalculator.calculateSubtotal(checkIn, checkOut, 249.90);
        assertEquals(599.76, subtotal, 0.01);
    }

    @Test
    public void testNonRefundableDiscount() {
        double subtotal = 779.80;
        double discount = PriceCalculator.calculateDiscount(subtotal, "NON_REFUNDABLE");
        assertEquals(116.97, discount, 0.01);
    }

    @Test
    public void testNights() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        int nights = PriceCalculator.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);
    }
}
