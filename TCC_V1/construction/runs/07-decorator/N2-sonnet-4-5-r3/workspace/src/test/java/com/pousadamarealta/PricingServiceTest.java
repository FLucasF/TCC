package com.pousadamarealta;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    void shouldCalculateRegularSeasonPrice() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate night = LocalDate.of(2027, 3, 10);

        BigDecimal price = pricingService.calculateNightPrice(baseRate, night);

        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void shouldCalculateHighSeasonPriceInJuly() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate night = LocalDate.of(2027, 7, 10);

        BigDecimal price = pricingService.calculateNightPrice(baseRate, night);

        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void shouldCalculateHighSeasonPriceInDecemberAfter15th() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate night = LocalDate.of(2027, 12, 20);

        BigDecimal price = pricingService.calculateNightPrice(baseRate, night);

        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void shouldCalculateRegularSeasonPriceInDecemberBefore15th() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate night = LocalDate.of(2027, 12, 10);

        BigDecimal price = pricingService.calculateNightPrice(baseRate, night);

        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void shouldCalculateSubtotalForTwoNights() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(baseRate, checkIn, checkOut);

        assertEquals(new BigDecimal("499.80"), subtotal);
    }

    @Test
    void shouldCalculateSubtotalForHighSeasonNights() {
        BigDecimal baseRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(baseRate, checkIn, checkOut);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void shouldCalculateNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    void shouldNotApplyDiscountForStandardPlan() {
        BigDecimal subtotal = new BigDecimal("779.80");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertEquals(new BigDecimal("0.00"), discount);
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("779.80");
        BigDecimal discount = new BigDecimal("116.97");

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        assertEquals(new BigDecimal("662.83"), total);
    }
}
