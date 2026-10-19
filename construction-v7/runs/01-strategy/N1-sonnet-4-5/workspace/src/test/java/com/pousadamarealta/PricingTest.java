package com.pousadamarealta;

import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;
import com.pousadamarealta.rateplan.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PricingTest {

    @Test
    public void testStandardPlanHighSeason() {
        Room room = new Room(101, RoomType.STANDARD);
        RatePlan plan = new StandardRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingResult result = plan.calculatePrice(room, checkIn, checkOut, null);

        assertEquals(new BigDecimal("599.76"), result.getSubtotal());
        assertEquals(new BigDecimal("0.00"), result.getDiscount());
        assertEquals(new BigDecimal("599.76"), result.getTotal());
    }

    @Test
    public void testCorporateTecnosul() {
        Room room = new Room(201, RoomType.DELUXE);
        RatePlan plan = new CorporateRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingResult result = plan.calculatePrice(room, checkIn, checkOut, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), result.getSubtotal());
        assertEquals(new BigDecimal("93.58"), result.getDiscount());
        assertEquals(new BigDecimal("686.22"), result.getTotal());
    }

    @Test
    public void testWeekendPackageDeluxe() {
        Room room = new Room(201, RoomType.DELUXE);
        RatePlan plan = new WeekendPackageRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        PricingResult result = plan.calculatePrice(room, checkIn, checkOut, null);

        assertEquals(new BigDecimal("820.00"), result.getSubtotal());
        assertEquals(new BigDecimal("0.00"), result.getDiscount());
        assertEquals(new BigDecimal("820.00"), result.getTotal());
    }

    @Test
    public void testWeekendPackageRefund() {
        RatePlan plan = new WeekendPackageRatePlan();
        BigDecimal amountPaid = new BigDecimal("820.00");

        BigDecimal refund = plan.calculateRefund(amountPaid);

        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    public void testLongStay() {
        Room room = new Room(101, RoomType.STANDARD);
        RatePlan plan = new LongStayRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        PricingResult result = plan.calculatePrice(room, checkIn, checkOut, null);

        assertEquals(new BigDecimal("1749.30"), result.getSubtotal());
        assertEquals(new BigDecimal("174.93"), result.getDiscount());
        assertEquals(new BigDecimal("1574.37"), result.getTotal());
    }
}
