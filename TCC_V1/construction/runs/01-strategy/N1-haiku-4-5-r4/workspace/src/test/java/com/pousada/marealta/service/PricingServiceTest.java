package com.pousada.marealta.service;

import com.pousada.marealta.model.Room;
import com.pousada.marealta.model.RoomType;
import com.pousada.marealta.model.RatePlan;
import org.junit.Test;
import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PricingServiceTest {
    private PricingService pricingService = new PricingService();

    @Test
    public void testStandardHighSeason() {
        Room room = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingService.PriceCalculation result = pricingService.calculatePrice(room, checkIn, checkOut, RatePlan.STANDARD, null);

        assertEquals(2, result.nights);
        assertEquals(0, new BigDecimal("599.76").compareTo(result.subtotal));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.discount));
        assertEquals(0, new BigDecimal("599.76").compareTo(result.total));
    }

    @Test
    public void testCorporateTecnosul() {
        Room room = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PriceCalculation result = pricingService.calculatePrice(room, checkIn, checkOut, RatePlan.CORPORATE, "TECNOSUL");

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("779.80"), result.subtotal);
        assertEquals(new BigDecimal("93.58"), result.discount);
        assertEquals(new BigDecimal("686.22"), result.total);
    }

    @Test
    public void testWeekendPackage() {
        Room room = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        PricingService.PriceCalculation result = pricingService.calculatePrice(room, checkIn, checkOut, RatePlan.WEEKEND_PACKAGE, null);

        assertEquals(2, result.nights);
        assertEquals(0, new BigDecimal("820.00").compareTo(result.subtotal));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.discount));
        assertEquals(0, new BigDecimal("820.00").compareTo(result.total));
    }

    @Test
    public void testLongStay() {
        Room room = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        PricingService.PriceCalculation result = pricingService.calculatePrice(room, checkIn, checkOut, RatePlan.LONG_STAY, null);

        assertEquals(7, result.nights);
        assertEquals(new BigDecimal("1749.30"), result.subtotal);
        assertEquals(new BigDecimal("174.93"), result.discount);
        assertEquals(new BigDecimal("1574.37"), result.total);
    }

    @Test
    public void testWeekendPackageValidation() {
        LocalDate friday = LocalDate.of(2027, 3, 12);
        LocalDate sunday = LocalDate.of(2027, 3, 14);
        assertTrue(PricingService.isWeekendPackageValid(friday, sunday));

        LocalDate thursday = LocalDate.of(2027, 3, 11);
        assertFalse(PricingService.isWeekendPackageValid(thursday, sunday));
    }

    @Test
    public void testRefundCalculations() {
        BigDecimal amountPaid = new BigDecimal("820.00");

        assertEquals(0, new BigDecimal("820.00").compareTo(PricingService.calculateRefund(RatePlan.STANDARD, amountPaid)));
        assertEquals(0, BigDecimal.ZERO.compareTo(PricingService.calculateRefund(RatePlan.NON_REFUNDABLE, amountPaid)));
        assertEquals(0, new BigDecimal("410.00").compareTo(PricingService.calculateRefund(RatePlan.WEEKEND_PACKAGE, amountPaid)));
        assertEquals(0, new BigDecimal("820.00").compareTo(PricingService.calculateRefund(RatePlan.CORPORATE, amountPaid)));
        assertEquals(0, new BigDecimal("820.00").compareTo(PricingService.calculateRefund(RatePlan.LONG_STAY, amountPaid)));
    }
}
