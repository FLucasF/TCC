package com.pousadamarealta.service;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    public void testNormalSeasonPrice() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate date = LocalDate.of(2027, 3, 10);

        BigDecimal price = pricingService.calculateNightPrice(room, date);

        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    public void testHighSeasonPrice() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate date = LocalDate.of(2027, 7, 10);

        BigDecimal price = pricingService.calculateNightPrice(room, date);

        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testSubtotalCalculation() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertEquals(new BigDecimal("116.97"), discount);
    }

    @Test
    public void testStandardPlanNoDiscount() {
        BigDecimal subtotal = new BigDecimal("779.80");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertEquals(new BigDecimal("0.00"), discount);
    }
}
