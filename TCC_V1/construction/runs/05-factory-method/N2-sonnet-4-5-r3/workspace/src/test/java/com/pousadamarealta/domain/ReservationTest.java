package com.pousadamarealta.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    @Test
    void testHighSeasonJuly() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 7, 10));
        reservation.setCheckOut(LocalDate.of(2027, 7, 12));
        reservation.setRatePlan(RatePlan.STANDARD);

        Room room = new Room("101", RoomType.STANDARD);

        BigDecimal subtotal = reservation.getSubtotal(room);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testLowSeason() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setRatePlan(RatePlan.STANDARD);

        Room room = new Room("201", RoomType.DELUXE);

        BigDecimal subtotal = reservation.getSubtotal(room);
        assertEquals(new BigDecimal("779.80"), subtotal);
    }

    @Test
    void testNonRefundableDiscount() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);

        Room room = new Room("201", RoomType.DELUXE);

        BigDecimal subtotal = reservation.getSubtotal(room);
        BigDecimal discount = reservation.getDiscount(subtotal);
        BigDecimal total = reservation.getTotal(room);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(new BigDecimal("116.97"), discount);
        assertEquals(new BigDecimal("662.83"), total);
    }

    @Test
    void testHighSeasonDecember() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 12, 15));
        reservation.setCheckOut(LocalDate.of(2027, 12, 17));
        reservation.setRatePlan(RatePlan.STANDARD);

        Room room = new Room("101", RoomType.STANDARD);

        BigDecimal subtotal = reservation.getSubtotal(room);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testStatusTransitions() {
        Reservation reservation = new Reservation();
        assertEquals(ReservationStatus.PENDING, reservation.getStatus());
        assertTrue(reservation.canConfirm());

        reservation.confirm(new BigDecimal("599.76"));
        assertEquals(ReservationStatus.CONFIRMED, reservation.getStatus());
        assertEquals(new BigDecimal("599.76"), reservation.getAmountPaid());
        assertTrue(reservation.canCheckIn());

        reservation.checkIn();
        assertEquals(ReservationStatus.CHECKED_IN, reservation.getStatus());
        assertTrue(reservation.canCheckOut());

        reservation.checkOut();
        assertEquals(ReservationStatus.CHECKED_OUT, reservation.getStatus());
    }

    @Test
    void testCancelStandardRefund() {
        Reservation reservation = new Reservation();
        reservation.setRatePlan(RatePlan.STANDARD);
        reservation.confirm(new BigDecimal("599.76"));

        reservation.cancel();
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(new BigDecimal("599.76"), reservation.getRefund());
    }

    @Test
    void testCancelNonRefundableNoRefund() {
        Reservation reservation = new Reservation();
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);
        reservation.confirm(new BigDecimal("662.83"));

        reservation.cancel();
        assertEquals(ReservationStatus.CANCELLED, reservation.getStatus());
        assertEquals(BigDecimal.ZERO, reservation.getRefund());
    }

    @Test
    void testGuestTypeIdentification() {
        Reservation individual = new Reservation();
        individual.setGuestCpf("111.222.333-44");
        assertTrue(individual.hasIndividualGuest());
        assertFalse(individual.hasCorporateGuest());
        assertFalse(individual.hasInternationalGuest());

        Reservation corporate = new Reservation();
        corporate.setCompanyCnpj("12.345.678/0001-90");
        corporate.setCompanyName("Tecnosul Ltda");
        assertFalse(corporate.hasIndividualGuest());
        assertTrue(corporate.hasCorporateGuest());
        assertFalse(corporate.hasInternationalGuest());

        Reservation international = new Reservation();
        international.setGuestPassport("X1234567");
        international.setGuestCountry("United Kingdom");
        assertFalse(international.hasIndividualGuest());
        assertFalse(international.hasCorporateGuest());
        assertTrue(international.hasInternationalGuest());
    }
}
