package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.exception.ApiException;
import com.pousadamarealta.model.Reservation;
import com.pousadamarealta.model.Room;
import com.pousadamarealta.model.RoomType;
import com.pousadamarealta.service.PricingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.junit.jupiter.api.Assertions.*;

class RatePlanTest {
    private final PricingService pricingService = new PricingService();
    private final Room standardRoom = new Room("101", RoomType.STANDARD);
    private final Room deluxeRoom = new Room("201", RoomType.DELUXE);

    @Test
    void testStandardRatePlan() {
        StandardRatePlan plan = new StandardRatePlan(pricingService);

        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setAmountPaid(new BigDecimal("499.80"));

        BigDecimal subtotal = plan.calculateSubtotal(reservation, standardRoom);
        assertEquals(new BigDecimal("499.80"), subtotal);

        BigDecimal discount = plan.calculateDiscount(subtotal, reservation);
        assertEquals(BigDecimal.ZERO, discount);

        BigDecimal refund = plan.calculateRefund(reservation);
        assertEquals(new BigDecimal("499.80"), refund);
    }

    @Test
    void testNonRefundableRatePlan() {
        NonRefundableRatePlan plan = new NonRefundableRatePlan(pricingService);

        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setAmountPaid(new BigDecimal("662.83"));

        BigDecimal subtotal = plan.calculateSubtotal(reservation, deluxeRoom);
        assertEquals(new BigDecimal("779.80"), subtotal);

        BigDecimal discount = plan.calculateDiscount(subtotal, reservation);
        assertEquals(new BigDecimal("116.97"), discount);

        BigDecimal refund = plan.calculateRefund(reservation);
        assertEquals(BigDecimal.ZERO, refund);
    }

    @Test
    void testCorporateRatePlanTecnosul() {
        CorporateRatePlan plan = new CorporateRatePlan(pricingService);

        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setCompanyCode("TECNOSUL");
        reservation.setAmountPaid(new BigDecimal("686.22"));

        BigDecimal subtotal = plan.calculateSubtotal(reservation, deluxeRoom);
        assertEquals(new BigDecimal("779.80"), subtotal);

        BigDecimal discount = plan.calculateDiscount(subtotal, reservation);
        assertEquals(new BigDecimal("93.58"), discount);

        BigDecimal refund = plan.calculateRefund(reservation);
        assertEquals(new BigDecimal("686.22"), refund);
    }

    @Test
    void testCorporateRatePlanInvalidCode() {
        CorporateRatePlan plan = new CorporateRatePlan(pricingService);

        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));
        reservation.setCompanyCode("INVALID");

        assertThrows(ApiException.class, () -> plan.validate(reservation, deluxeRoom));
    }

    @Test
    void testWeekendPackage() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan(pricingService);

        LocalDate friday = LocalDate.of(2027, 3, 1)
            .with(TemporalAdjusters.nextOrSame(DayOfWeek.FRIDAY));
        LocalDate sunday = friday.plusDays(2);

        Reservation reservation = new Reservation();
        reservation.setCheckIn(friday);
        reservation.setCheckOut(sunday);
        reservation.setAmountPaid(new BigDecimal("820.00"));

        plan.validate(reservation, deluxeRoom);

        BigDecimal subtotal = plan.calculateSubtotal(reservation, deluxeRoom);
        assertEquals(new BigDecimal("820.00"), subtotal);

        BigDecimal discount = plan.calculateDiscount(subtotal, reservation);
        assertEquals(BigDecimal.ZERO, discount);

        BigDecimal refund = plan.calculateRefund(reservation);
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    void testWeekendPackageInvalidDates() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan(pricingService);

        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 10));
        reservation.setCheckOut(LocalDate.of(2027, 3, 12));

        assertThrows(ApiException.class, () -> plan.validate(reservation, deluxeRoom));
    }

    @Test
    void testLongStay() {
        LongStayRatePlan plan = new LongStayRatePlan(pricingService);

        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 1));
        reservation.setCheckOut(LocalDate.of(2027, 3, 8));
        reservation.setAmountPaid(new BigDecimal("1574.37"));

        plan.validate(reservation, standardRoom);

        BigDecimal subtotal = plan.calculateSubtotal(reservation, standardRoom);
        assertEquals(new BigDecimal("1749.30"), subtotal);

        BigDecimal discount = plan.calculateDiscount(subtotal, reservation);
        assertEquals(new BigDecimal("174.93"), discount);

        BigDecimal refund = plan.calculateRefund(reservation);
        assertEquals(new BigDecimal("1574.37"), refund);
    }

    @Test
    void testLongStayInsufficientNights() {
        LongStayRatePlan plan = new LongStayRatePlan(pricingService);

        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 3, 1));
        reservation.setCheckOut(LocalDate.of(2027, 3, 6));

        assertThrows(ApiException.class, () -> plan.validate(reservation, standardRoom));
    }

    @Test
    void testHighSeasonCalculation() {
        StandardRatePlan plan = new StandardRatePlan(pricingService);

        Reservation reservation = new Reservation();
        reservation.setCheckIn(LocalDate.of(2027, 7, 10));
        reservation.setCheckOut(LocalDate.of(2027, 7, 12));

        BigDecimal subtotal = plan.calculateSubtotal(reservation, standardRoom);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }
}
