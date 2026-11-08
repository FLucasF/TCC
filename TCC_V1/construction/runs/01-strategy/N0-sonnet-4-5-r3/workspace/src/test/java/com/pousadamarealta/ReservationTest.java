package com.pousadamarealta;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.service.rateplan.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testStandardHighSeason() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("101");
        reservation.setCheckIn(LocalDate.of(2027, 7, 10));
        reservation.setCheckOut(LocalDate.of(2027, 7, 12));
        reservation.setNights(2);

        Room room = new Room("101", RoomType.STANDARD);

        StandardRatePlan ratePlan = new StandardRatePlan();
        ratePlan.calculatePricing(reservation, room);

        assertEquals(new BigDecimal("599.76"), reservation.getSubtotal());
        assertEquals(new BigDecimal("0.00"), reservation.getDiscount());
        assertEquals(new BigDecimal("599.76"), reservation.getTotal());
    }

    @Test
    void testCorporateTecnosulDeluxe() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("201");
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setNights(2);
        reservation.setCompanyCode("TECNOSUL");

        Room room = new Room("201", RoomType.DELUXE);

        CorporateRatePlan ratePlan = new CorporateRatePlan();
        ratePlan.calculatePricing(reservation, room);

        assertEquals(new BigDecimal("779.80"), reservation.getSubtotal());
        assertEquals(new BigDecimal("93.58"), reservation.getDiscount());
        assertEquals(new BigDecimal("686.22"), reservation.getTotal());
    }

    @Test
    void testWeekendPackageDeluxe() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("201");
        reservation.setCheckIn(LocalDate.of(2027, 3, 12)); // Friday
        reservation.setCheckOut(LocalDate.of(2027, 3, 14)); // Sunday
        reservation.setNights(2);

        Room room = new Room("201", RoomType.DELUXE);

        WeekendPackageRatePlan ratePlan = new WeekendPackageRatePlan();
        ratePlan.calculatePricing(reservation, room);

        assertEquals(new BigDecimal("820.00"), reservation.getSubtotal());
        assertEquals(new BigDecimal("820.00"), reservation.getTotal());

        reservation.setAmountPaid(new BigDecimal("820.00"));
        BigDecimal refund = ratePlan.calculateRefund(reservation);
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    void testLongStayStandard() {
        Reservation reservation = new Reservation();
        reservation.setRoomNumber("101");
        reservation.setCheckIn(LocalDate.of(2027, 3, 1));
        reservation.setCheckOut(LocalDate.of(2027, 3, 8));
        reservation.setNights(7);

        Room room = new Room("101", RoomType.STANDARD);

        LongStayRatePlan ratePlan = new LongStayRatePlan();
        ratePlan.calculatePricing(reservation, room);

        assertEquals(new BigDecimal("1749.30"), reservation.getSubtotal());
        assertEquals(new BigDecimal("174.93"), reservation.getDiscount());
        assertEquals(new BigDecimal("1574.37"), reservation.getTotal());
    }

    @Test
    void testNonRefundableRefund() {
        Reservation reservation = new Reservation();
        reservation.setAmountPaid(new BigDecimal("500.00"));

        NonRefundableRatePlan ratePlan = new NonRefundableRatePlan();
        BigDecimal refund = ratePlan.calculateRefund(reservation);

        assertEquals(BigDecimal.ZERO, refund);
    }

    @Test
    void testStandardRefund() {
        Reservation reservation = new Reservation();
        reservation.setAmountPaid(new BigDecimal("500.00"));

        StandardRatePlan ratePlan = new StandardRatePlan();
        BigDecimal refund = ratePlan.calculateRefund(reservation);

        assertEquals(new BigDecimal("500.00"), refund);
    }
}
