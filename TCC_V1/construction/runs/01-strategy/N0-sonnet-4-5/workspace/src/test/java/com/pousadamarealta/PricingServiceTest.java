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
    void testStandardRoomOutsideHighSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PriceBreakdown price = pricingService.calculatePrice(
                room, checkIn, checkOut, RatePlan.STANDARD, null);

        assertEquals(new BigDecimal("499.80"), price.subtotal);
        assertEquals(new BigDecimal("0.00"), price.discount);
        assertEquals(new BigDecimal("499.80"), price.total);
    }

    @Test
    void testStandardRoomInHighSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingService.PriceBreakdown price = pricingService.calculatePrice(
                room, checkIn, checkOut, RatePlan.STANDARD, null);

        assertEquals(new BigDecimal("599.76"), price.subtotal);
        assertEquals(new BigDecimal("0.00"), price.discount);
        assertEquals(new BigDecimal("599.76"), price.total);
    }

    @Test
    void testCorporateRateTecnosul() {
        Room room = new Room("201", RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PriceBreakdown price = pricingService.calculatePrice(
                room, checkIn, checkOut, RatePlan.CORPORATE, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), price.subtotal);
        assertEquals(new BigDecimal("93.58"), price.discount);
        assertEquals(new BigDecimal("686.22"), price.total);
    }

    @Test
    void testWeekendPackageDeluxe() {
        Room room = new Room("201", RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 3, 12); // Friday
        LocalDate checkOut = LocalDate.of(2027, 3, 14); // Sunday

        PricingService.PriceBreakdown price = pricingService.calculatePrice(
                room, checkIn, checkOut, RatePlan.WEEKEND_PACKAGE, null);

        assertEquals(new BigDecimal("820.00"), price.subtotal);
        assertEquals(new BigDecimal("0.00"), price.discount);
        assertEquals(new BigDecimal("820.00"), price.total);
    }

    @Test
    void testLongStay() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        PricingService.PriceBreakdown price = pricingService.calculatePrice(
                room, checkIn, checkOut, RatePlan.LONG_STAY, null);

        assertEquals(new BigDecimal("1749.30"), price.subtotal);
        assertEquals(new BigDecimal("174.93"), price.discount);
        assertEquals(new BigDecimal("1574.37"), price.total);
    }

    @Test
    void testWeekendPackageCancellationRefund() {
        BigDecimal paid = new BigDecimal("820.00");
        BigDecimal refund = pricingService.calculateRefund(paid, RatePlan.WEEKEND_PACKAGE);
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    void testNonRefundableNoRefund() {
        BigDecimal paid = new BigDecimal("500.00");
        BigDecimal refund = pricingService.calculateRefund(paid, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("0.00"), refund);
    }

    @Test
    void testStandardFullRefund() {
        BigDecimal paid = new BigDecimal("500.00");
        BigDecimal refund = pricingService.calculateRefund(paid, RatePlan.STANDARD);
        assertEquals(new BigDecimal("500.00"), refund);
    }

    @Test
    void testValidateWeekendPackageNotFriday() {
        LocalDate checkIn = LocalDate.of(2027, 3, 13); // Saturday
        LocalDate checkOut = LocalDate.of(2027, 3, 15);

        assertThrows(IllegalArgumentException.class, () ->
                pricingService.validateRatePlan(RatePlan.WEEKEND_PACKAGE, checkIn, checkOut, null)
        );
    }

    @Test
    void testValidateWeekendPackageNotTwoNights() {
        LocalDate checkIn = LocalDate.of(2027, 3, 12); // Friday
        LocalDate checkOut = LocalDate.of(2027, 3, 16); // Too many nights

        assertThrows(IllegalArgumentException.class, () ->
                pricingService.validateRatePlan(RatePlan.WEEKEND_PACKAGE, checkIn, checkOut, null)
        );
    }

    @Test
    void testValidateLongStayTooShort() {
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 7); // Only 6 nights

        assertThrows(IllegalArgumentException.class, () ->
                pricingService.validateRatePlan(RatePlan.LONG_STAY, checkIn, checkOut, null)
        );
    }

    @Test
    void testValidateCorporateInvalidCompany() {
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 3);

        assertThrows(IllegalArgumentException.class, () ->
                pricingService.validateRatePlan(RatePlan.CORPORATE, checkIn, checkOut, "INVALID")
        );
    }
}
