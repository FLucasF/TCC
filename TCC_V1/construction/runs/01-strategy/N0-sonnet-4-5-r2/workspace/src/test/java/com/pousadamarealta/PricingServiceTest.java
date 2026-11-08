package com.pousadamarealta;

import com.pousadamarealta.domain.*;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PricingServiceTest {

    private PricingService pricingService;

    @BeforeEach
    void setup() {
        pricingService = new PricingService();
    }

    @Test
    void testHighSeasonStandard() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 7, 10));
        reservation.setCheckOut(LocalDate.of(2027, 7, 12));
        reservation.setRatePlan(RatePlan.STANDARD);

        Room room = new Room("101", RoomType.STANDARD);
        pricingService.calculatePricing(reservation, room);

        assertEquals(0, new BigDecimal("599.76").compareTo(reservation.getSubtotal()));
        assertEquals(0, new BigDecimal("0.00").compareTo(reservation.getDiscount()));
        assertEquals(0, new BigDecimal("599.76").compareTo(reservation.getTotal()));
    }

    @Test
    void testCorporateTecnosul() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setRatePlan(RatePlan.CORPORATE);
        reservation.setCompanyCode("TECNOSUL");

        Room room = new Room("201", RoomType.DELUXE);
        pricingService.calculatePricing(reservation, room);

        assertEquals(0, new BigDecimal("779.80").compareTo(reservation.getSubtotal()));
        assertEquals(0, new BigDecimal("93.58").compareTo(reservation.getDiscount()));
        assertEquals(0, new BigDecimal("686.22").compareTo(reservation.getTotal()));
    }

    @Test
    void testWeekendPackageDeluxe() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 12));
        reservation.setCheckOut(LocalDate.of(2027, 3, 14));
        reservation.setRatePlan(RatePlan.WEEKEND_PACKAGE);

        Room room = new Room("201", RoomType.DELUXE);
        pricingService.calculatePricing(reservation, room);

        assertEquals(0, new BigDecimal("820.00").compareTo(reservation.getSubtotal()));
        assertEquals(0, new BigDecimal("0.00").compareTo(reservation.getDiscount()));
        assertEquals(0, new BigDecimal("820.00").compareTo(reservation.getTotal()));
    }

    @Test
    void testWeekendPackageRefund() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 12));
        reservation.setCheckOut(LocalDate.of(2027, 3, 14));
        reservation.setRatePlan(RatePlan.WEEKEND_PACKAGE);
        reservation.setAmountPaid(new BigDecimal("820.00"));

        BigDecimal refund = pricingService.calculateRefund(reservation);
        assertEquals(0, new BigDecimal("410.00").compareTo(refund));
    }

    @Test
    void testLongStayStandard() {
        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 1));
        reservation.setCheckOut(LocalDate.of(2027, 3, 8));
        reservation.setRatePlan(RatePlan.LONG_STAY);

        Room room = new Room("101", RoomType.STANDARD);
        pricingService.calculatePricing(reservation, room);

        assertEquals(0, new BigDecimal("1749.30").compareTo(reservation.getSubtotal()));
        assertEquals(0, new BigDecimal("174.93").compareTo(reservation.getDiscount()));
        assertEquals(0, new BigDecimal("1574.37").compareTo(reservation.getTotal()));
    }

    @Test
    void testNonRefundableCancellation() {
        Reservation reservation = new Reservation();
        reservation.setRatePlan(RatePlan.NON_REFUNDABLE);
        reservation.setAmountPaid(new BigDecimal("424.83"));

        BigDecimal refund = pricingService.calculateRefund(reservation);
        assertEquals(0, new BigDecimal("0.00").compareTo(refund));
    }

    @Test
    void testStandardCancellationFullRefund() {
        Reservation reservation = new Reservation();
        reservation.setRatePlan(RatePlan.STANDARD);
        reservation.setAmountPaid(new BigDecimal("779.80"));

        BigDecimal refund = pricingService.calculateRefund(reservation);
        assertEquals(0, new BigDecimal("779.80").compareTo(refund));
    }

    @Test
    void testPendingCancellationNoRefund() {
        Reservation reservation = new Reservation();
        reservation.setRatePlan(RatePlan.STANDARD);
        reservation.setAmountPaid(new BigDecimal("0.00"));

        BigDecimal refund = pricingService.calculateRefund(reservation);
        assertEquals(0, new BigDecimal("0.00").compareTo(refund));
    }
}
