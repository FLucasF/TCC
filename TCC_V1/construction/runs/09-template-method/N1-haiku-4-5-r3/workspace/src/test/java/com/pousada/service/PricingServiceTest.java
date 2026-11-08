package com.pousada.service;

import com.pousada.model.Room;
import com.pousada.model.RoomType;
import com.pousada.model.RatePlan;
import org.junit.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.assertEquals;

public class PricingServiceTest {
    @Test
    public void testStandardRateLowSeason() {
        Room room = new Room(101, RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        RatePlan plan = RatePlan.STANDARD;

        PricingService.PricingResult result = PricingService.calculatePrice(room, checkIn, checkOut, plan);

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("499.80"), result.subtotal);
        assertEquals(new BigDecimal("0.00"), result.discount);
        assertEquals(new BigDecimal("499.80"), result.total);
    }

    @Test
    public void testStandardRateHighSeason() {
        Room room = new Room(101, RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        RatePlan plan = RatePlan.STANDARD;

        PricingService.PricingResult result = PricingService.calculatePrice(room, checkIn, checkOut, plan);

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("599.76"), result.subtotal);
        assertEquals(new BigDecimal("0.00"), result.discount);
        assertEquals(new BigDecimal("599.76"), result.total);
    }

    @Test
    public void testNonRefundableRateLowSeason() {
        Room room = new Room(101, RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        RatePlan plan = RatePlan.NON_REFUNDABLE;

        PricingService.PricingResult result = PricingService.calculatePrice(room, checkIn, checkOut, plan);

        assertEquals(2, result.nights);
        assertEquals(new BigDecimal("499.80"), result.subtotal);
        assertEquals(new BigDecimal("74.97"), result.discount);
        assertEquals(new BigDecimal("424.83"), result.total);
    }

    @Test
    public void testDeluxeRoomHighSeason() {
        Room room = new Room(201, RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 13);
        RatePlan plan = RatePlan.STANDARD;

        PricingService.PricingResult result = PricingService.calculatePrice(room, checkIn, checkOut, plan);

        assertEquals(3, result.nights);
        assertEquals(new BigDecimal("1403.64"), result.subtotal);
        assertEquals(new BigDecimal("0.00"), result.discount);
        assertEquals(new BigDecimal("1403.64"), result.total);
    }

    @Test
    public void testSuiteRoomNonRefundable() {
        Room room = new Room(301, RoomType.SUITE);
        LocalDate checkIn = LocalDate.of(2027, 12, 20);
        LocalDate checkOut = LocalDate.of(2027, 12, 23);
        RatePlan plan = RatePlan.NON_REFUNDABLE;

        PricingService.PricingResult result = PricingService.calculatePrice(room, checkIn, checkOut, plan);

        assertEquals(3, result.nights);
        assertEquals(new BigDecimal("2087.64"), result.subtotal);
        assertEquals(new BigDecimal("313.15"), result.discount);
        assertEquals(new BigDecimal("1774.49"), result.total);
    }
}
