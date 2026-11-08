package com.pousadamarealta;

import com.pousadamarealta.model.RatePlan;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {

    private final PricingService pricingService = new PricingService();

    @Test
    void testStandardRoomInHighSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingService.PricingResult result = pricingService.calculatePrice(
            room, checkIn, checkOut, RatePlan.STANDARD, null
        );

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("599.76"), result.subtotal);
        assertEquals(BigDecimal.ZERO, result.discount);
        assertEquals(new BigDecimal("599.76"), result.total);
    }

    @Test
    void testDeluxeOutsideHighSeason() {
        Room room = new Room("201", RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PricingResult result = pricingService.calculatePrice(
            room, checkIn, checkOut, RatePlan.STANDARD, null
        );

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("779.80"), result.subtotal);
        assertEquals(BigDecimal.ZERO, result.discount);
        assertEquals(new BigDecimal("779.80"), result.total);
    }

    @Test
    void testCorporateTecnosul() {
        Room room = new Room("201", RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PricingResult result = pricingService.calculatePrice(
            room, checkIn, checkOut, RatePlan.CORPORATE, "TECNOSUL"
        );

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("779.80"), result.subtotal);
        assertEquals(new BigDecimal("93.58"), result.discount);
        assertEquals(new BigDecimal("686.22"), result.total);
    }

    @Test
    void testWeekendPackageDeluxe() {
        Room room = new Room("201", RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        PricingService.PricingResult result = pricingService.calculatePrice(
            room, checkIn, checkOut, RatePlan.WEEKEND_PACKAGE, null
        );

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("820.00"), result.subtotal);
        assertEquals(BigDecimal.ZERO, result.discount);
        assertEquals(new BigDecimal("820.00"), result.total);
    }

    @Test
    void testWeekendPackageRefund() {
        BigDecimal paid = new BigDecimal("820.00");
        BigDecimal refund = pricingService.calculateRefund(RatePlan.WEEKEND_PACKAGE, paid);
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    void testLongStayStandard() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        PricingService.PricingResult result = pricingService.calculatePrice(
            room, checkIn, checkOut, RatePlan.LONG_STAY, null
        );

        assertEquals(7, result.nights);
        assertEquals(new BigDecimal("1749.30"), result.subtotal);
        assertEquals(new BigDecimal("174.93"), result.discount);
        assertEquals(new BigDecimal("1574.37"), result.total);
    }
}
