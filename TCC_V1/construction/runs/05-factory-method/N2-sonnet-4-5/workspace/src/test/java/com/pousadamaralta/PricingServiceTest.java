package com.pousadamaralta;

import com.pousadamaralta.domain.model.Room;
import com.pousadamaralta.domain.model.RoomType;
import com.pousadamaralta.domain.model.RatePlanType;
import com.pousadamaralta.domain.service.PricingService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    void testHighSeasonPricing() {
        Room room = new Room("101", RoomType.STANDARD);

        LocalDate julyDate = LocalDate.of(2027, 7, 10);
        BigDecimal nightPrice = pricingService.calculateNightPrice(room, julyDate);

        assertEquals(new BigDecimal("299.88"), nightPrice);
    }

    @Test
    void testLowSeasonPricing() {
        Room room = new Room("101", RoomType.STANDARD);

        LocalDate marchDate = LocalDate.of(2027, 3, 10);
        BigDecimal nightPrice = pricingService.calculateNightPrice(room, marchDate);

        assertEquals(new BigDecimal("249.90"), nightPrice);
    }

    @Test
    void testSubtotalCalculation() {
        Room room = new Room("101", RoomType.STANDARD);

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal subtotal = pricingService.calculateSubtotal(room, checkIn, checkOut);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(RatePlanType.NON_REFUNDABLE, subtotal);

        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    void testStandardNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = pricingService.calculateDiscount(RatePlanType.STANDARD, subtotal);

        assertEquals(BigDecimal.ZERO, discount);
    }
}
