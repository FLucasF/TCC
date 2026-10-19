package com.pousada.domain.rateplan;

import com.pousada.domain.PriceCalculation;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RatePlanTest {

    @Test
    public void testStandardRatePlanHighSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        StandardRatePlan ratePlan = new StandardRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PriceCalculation price = ratePlan.calculatePrice(room, checkIn, checkOut, 2);

        assertEquals(new BigDecimal("599.76"), price.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2), price.getDiscount());
        assertEquals(new BigDecimal("599.76"), price.getTotal());
    }

    @Test
    public void testCorporateRatePlanTecnosul() {
        Room room = new Room("201", RoomType.DELUXE);
        CorporateRatePlan ratePlan = new CorporateRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PriceCalculation price = ratePlan.calculatePriceWithCompany(room, checkIn, 2, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), price.getSubtotal());
        assertEquals(new BigDecimal("93.58"), price.getDiscount());
        assertEquals(new BigDecimal("686.22"), price.getTotal());
    }

    @Test
    public void testWeekendPackageDeluxe() {
        Room room = new Room("201", RoomType.DELUXE);
        WeekendPackageRatePlan ratePlan = new WeekendPackageRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        PriceCalculation price = ratePlan.calculatePrice(room, checkIn, checkOut, 2);

        assertEquals(new BigDecimal("820.00"), price.getSubtotal());
        assertEquals(BigDecimal.ZERO.setScale(2), price.getDiscount());
        assertEquals(new BigDecimal("820.00"), price.getTotal());

        BigDecimal refund = ratePlan.calculateRefund(new BigDecimal("820.00"));
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    public void testLongStayRatePlan() {
        Room room = new Room("101", RoomType.STANDARD);
        LongStayRatePlan ratePlan = new LongStayRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        PriceCalculation price = ratePlan.calculatePrice(room, checkIn, checkOut, 7);

        assertEquals(new BigDecimal("1749.30"), price.getSubtotal());
        assertEquals(new BigDecimal("174.93"), price.getDiscount());
        assertEquals(new BigDecimal("1574.37"), price.getTotal());
    }
}
