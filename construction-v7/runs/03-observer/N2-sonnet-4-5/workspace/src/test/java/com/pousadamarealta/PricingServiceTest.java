package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    void shouldCalculateNightlyPriceInLowSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate date = LocalDate.of(2027, 3, 10);

        BigDecimal price = pricingService.calculateNightlyPrice(room, date);

        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void shouldCalculateNightlyPriceInHighSeasonJuly() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate date = LocalDate.of(2027, 7, 10);

        BigDecimal price = pricingService.calculateNightlyPrice(room, date);

        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void shouldCalculateSubtotalForMultipleNights() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    void shouldNotCalculateDiscountForStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");

        BigDecimal total = pricingService.calculateTotal(subtotal, discount);

        assertEquals(new BigDecimal("509.80"), total);
    }
}
