package com.pousadamarealta.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RatePlanTest {

    @Test
    void testStandardRatePlan() {
        StandardRatePlan plan = new StandardRatePlan();
        Room room = new Room("101", RoomType.STANDARD);

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingResult result = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(new BigDecimal("499.80"), result.getSubtotal());
        assertEquals(BigDecimal.ZERO, result.getDiscount());
        assertEquals(new BigDecimal("499.80"), result.getTotal());
    }

    @Test
    void testStandardRatePlanHighSeason() {
        StandardRatePlan plan = new StandardRatePlan();
        Room room = new Room("101", RoomType.STANDARD);

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingResult result = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(new BigDecimal("599.76"), result.getSubtotal());
        assertEquals(BigDecimal.ZERO, result.getDiscount());
        assertEquals(new BigDecimal("599.76"), result.getTotal());
    }

    @Test
    void testNonRefundableRatePlan() {
        NonRefundableRatePlan plan = new NonRefundableRatePlan();
        Room room = new Room("101", RoomType.STANDARD);

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingResult result = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(new BigDecimal("499.80"), result.getSubtotal());
        assertEquals(new BigDecimal("74.97"), result.getDiscount());
        assertEquals(new BigDecimal("424.83"), result.getTotal());
    }

    @Test
    void testCorporateRatePlanTecnosul() {
        CorporateRatePlan plan = new CorporateRatePlan();
        Room room = new Room("201", RoomType.DELUXE);

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingResult result = plan.calculatePriceWithCompanyCode(room, checkIn, checkOut, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), result.getSubtotal());
        assertEquals(new BigDecimal("93.58"), result.getDiscount());
        assertEquals(new BigDecimal("686.22"), result.getTotal());
    }

    @Test
    void testCorporateRatePlanInvalidCompanyCode() {
        CorporateRatePlan plan = new CorporateRatePlan();
        Room room = new Room("201", RoomType.DELUXE);

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(InvalidRequestException.class, () -> {
            plan.validate(room, checkIn, checkOut, 2, "INVALID");
        });
    }

    @Test
    void testWeekendPackageDeluxe() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();
        Room room = new Room("201", RoomType.DELUXE);

        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        PricingResult result = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(new BigDecimal("820.00"), result.getSubtotal());
        assertEquals(BigDecimal.ZERO, result.getDiscount());
        assertEquals(new BigDecimal("820.00"), result.getTotal());

        BigDecimal refund = plan.calculateRefund(new BigDecimal("820.00"));
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    void testWeekendPackageNotFriday() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();
        Room room = new Room("201", RoomType.DELUXE);

        LocalDate checkIn = LocalDate.of(2027, 3, 13);
        LocalDate checkOut = LocalDate.of(2027, 3, 15);

        assertThrows(InvalidRequestException.class, () -> {
            plan.validate(room, checkIn, checkOut, 2, null);
        });
    }

    @Test
    void testLongStayRatePlan() {
        LongStayRatePlan plan = new LongStayRatePlan();
        Room room = new Room("101", RoomType.STANDARD);

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        PricingResult result = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(new BigDecimal("1749.30"), result.getSubtotal());
        assertEquals(new BigDecimal("174.93"), result.getDiscount());
        assertEquals(new BigDecimal("1574.37"), result.getTotal());
    }

    @Test
    void testLongStayRatePlanLessThan7Nights() {
        LongStayRatePlan plan = new LongStayRatePlan();
        Room room = new Room("101", RoomType.STANDARD);

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 7);

        assertThrows(InvalidRequestException.class, () -> {
            plan.validate(room, checkIn, checkOut, 2, null);
        });
    }
}
