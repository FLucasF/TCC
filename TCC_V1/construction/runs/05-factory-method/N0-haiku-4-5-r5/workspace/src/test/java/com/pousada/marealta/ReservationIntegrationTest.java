package com.pousada.marealta;

import com.pousada.marealta.model.Reservation;
import com.pousada.marealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class ReservationIntegrationTest {

    @Test
    public void testHighSeasonPricing() {
        // July 10-12, 2027 (high season)
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        // STANDARD room: R$ 249.90
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal subtotal = PricingService.calculateSubtotal(baseRate, checkIn, checkOut);

        // Each night costs 249.90 * 1.20 = 299.88
        // 2 nights: 299.88 * 2 = 599.76
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testNonHighSeasonPricing() {
        // March 10-12, 2027 (not high season)
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal baseRate = new BigDecimal("389.90");
        BigDecimal subtotal = PricingService.calculateSubtotal(baseRate, checkIn, checkOut);

        // Each night costs 389.90
        // 2 nights: 389.90 * 2 = 779.80
        assertEquals(new BigDecimal("779.80"), subtotal);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PricingService.calculateDiscount(subtotal, true);

        // 15% discount: 599.76 * 0.15 = 89.964 -> 89.96 (banker's rounding)
        assertEquals(new BigDecimal("89.96"), discount);

        BigDecimal total = PricingService.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }

    @Test
    public void testStandardRateNoDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = PricingService.calculateDiscount(subtotal, false);

        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN), discount);

        BigDecimal total = PricingService.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("779.80"), total);
    }

    @Test
    public void testDecemberHighSeason() {
        // December 20, 2027 (high season starts from Dec 15)
        LocalDate date = LocalDate.of(2027, 12, 20);
        BigDecimal baseRate = new BigDecimal("249.90");
        BigDecimal nightPrice = PricingService.calculateNightlyPrice(baseRate, date);

        // 249.90 * 1.20 = 299.88
        assertEquals(new BigDecimal("299.88"), nightPrice);
    }

    @Test
    public void testFebruaryHighSeason() {
        // February 15, 2027 (high season)
        LocalDate date = LocalDate.of(2027, 2, 15);
        BigDecimal baseRate = new BigDecimal("389.90");
        BigDecimal nightPrice = PricingService.calculateNightlyPrice(baseRate, date);

        // 389.90 * 1.20 = 467.88
        assertEquals(new BigDecimal("467.88"), nightPrice);
    }

    @Test
    public void testNightCount() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        int nights = PricingService.calculateNights(checkIn, checkOut);
        assertEquals(2, nights);
    }

    @Test
    public void testIssRetention() {
        BigDecimal total = new BigDecimal("779.80");
        BigDecimal iss = PricingService.calculateIssRetention(total);

        // 5% of 779.80 = 38.99
        assertEquals(new BigDecimal("38.99"), iss);
    }

    @Test
    public void testBankerRounding() {
        // Test round half even - rounds to even number
        BigDecimal value = new BigDecimal("89.964");
        BigDecimal rounded = value.setScale(2, RoundingMode.HALF_EVEN);
        assertEquals(new BigDecimal("89.96"), rounded);

        // Test round half even - 89.965 rounds to 89.96 (even) not 89.97 (odd)
        BigDecimal value2 = new BigDecimal("89.965");
        BigDecimal rounded2 = value2.setScale(2, RoundingMode.HALF_EVEN);
        assertEquals(new BigDecimal("89.96"), rounded2);

        // Test round half even - 89.975 rounds to 89.98 (even)
        BigDecimal value3 = new BigDecimal("89.975");
        BigDecimal rounded3 = value3.setScale(2, RoundingMode.HALF_EVEN);
        assertEquals(new BigDecimal("89.98"), rounded3);
    }
}
