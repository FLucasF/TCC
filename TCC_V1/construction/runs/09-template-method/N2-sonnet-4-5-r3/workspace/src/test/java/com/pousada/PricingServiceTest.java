package com.pousada;

import com.pousada.domain.rateplan.NonRefundableRatePlan;
import com.pousada.domain.rateplan.StandardRatePlan;
import com.pousada.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void shouldCalculateSubtotalForHighSeason() {
        BigDecimal nightlyRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(nightlyRate, checkIn, checkOut);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void shouldCalculateSubtotalForLowSeason() {
        BigDecimal nightlyRate = new BigDecimal("389.90");
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(nightlyRate, checkIn, checkOut);

        assertEquals(new BigDecimal("779.80"), subtotal);
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, new NonRefundableRatePlan());

        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    void shouldCalculateNoDiscountForStandard() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = pricingService.calculateDiscount(subtotal, new StandardRatePlan());

        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");
        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        assertEquals(new BigDecimal("662.83"), total);
    }
}
