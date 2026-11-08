package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingServiceTest {
    private final PricingService pricingService = new PricingService();

    @Test
    public void testLowSeasonPrice() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate date = LocalDate.of(2027, 3, 15);

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
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testStandardPlanNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");

        BigDecimal discount = pricingService.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    public void testTourismTaxCalculation() {
        BigDecimal tax = pricingService.calculateTourismTax(2, 3);

        assertEquals(new BigDecimal("27.00"), tax);
    }
}
