package com.pousadamarealta.domain.service.rateplan;

import com.pousadamarealta.domain.model.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RatePlanTest {

    @Test
    void testStandardRatePlan() {
        StandardRatePlan plan = new StandardRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = plan.calculateSubtotal(RoomType.DELUXE, checkIn, checkOut);
        BigDecimal discount = plan.calculateDiscount(subtotal, RoomType.DELUXE, checkIn, checkOut);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(BigDecimal.ZERO, discount);
        assertEquals(new BigDecimal("779.80"), plan.calculateRefund(new BigDecimal("779.80")));
    }

    @Test
    void testNonRefundableRatePlan() {
        NonRefundableRatePlan plan = new NonRefundableRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = plan.calculateSubtotal(RoomType.DELUXE, checkIn, checkOut);
        BigDecimal discount = plan.calculateDiscount(subtotal, RoomType.DELUXE, checkIn, checkOut);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(new BigDecimal("116.97"), discount);
        assertEquals(BigDecimal.ZERO, plan.calculateRefund(new BigDecimal("662.83")));
    }

    @Test
    void testCorporateRatePlan() {
        CorporateRatePlan plan = new CorporateRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertDoesNotThrow(() -> plan.validate(checkIn, checkOut, "TECNOSUL"));

        BigDecimal subtotal = plan.calculateSubtotal(RoomType.DELUXE, checkIn, checkOut);
        BigDecimal discount = plan.calculateDiscount(subtotal, "TECNOSUL");

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(new BigDecimal("93.58"), discount);
    }

    @Test
    void testCorporateRatePlanInvalidCompany() {
        CorporateRatePlan plan = new CorporateRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(IllegalArgumentException.class, () ->
            plan.validate(checkIn, checkOut, "INVALID")
        );
    }

    @Test
    void testWeekendPackageRatePlan() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        assertDoesNotThrow(() -> plan.validate(checkIn, checkOut, null));

        BigDecimal subtotal = plan.calculateSubtotal(RoomType.DELUXE, checkIn, checkOut);
        BigDecimal discount = plan.calculateDiscount(subtotal, RoomType.DELUXE, checkIn, checkOut);

        assertEquals(new BigDecimal("820.00"), subtotal);
        assertEquals(BigDecimal.ZERO, discount);
        assertEquals(new BigDecimal("410.00"), plan.calculateRefund(new BigDecimal("820.00")));
    }

    @Test
    void testWeekendPackageInvalidDates() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(IllegalArgumentException.class, () ->
            plan.validate(checkIn, checkOut, null)
        );
    }

    @Test
    void testLongStayRatePlan() {
        LongStayRatePlan plan = new LongStayRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        assertDoesNotThrow(() -> plan.validate(checkIn, checkOut, null));

        BigDecimal subtotal = plan.calculateSubtotal(RoomType.STANDARD, checkIn, checkOut);
        BigDecimal discount = plan.calculateDiscount(subtotal, RoomType.STANDARD, checkIn, checkOut);

        assertEquals(new BigDecimal("1749.30"), subtotal);
        assertEquals(new BigDecimal("174.93"), discount);
        assertEquals(new BigDecimal("1574.37"), plan.calculateRefund(new BigDecimal("1574.37")));
    }

    @Test
    void testLongStayInsufficientNights() {
        LongStayRatePlan plan = new LongStayRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 7);

        assertThrows(IllegalArgumentException.class, () ->
            plan.validate(checkIn, checkOut, null)
        );
    }

    @Test
    void testHighSeasonCalculation() {
        StandardRatePlan plan = new StandardRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = plan.calculateSubtotal(RoomType.STANDARD, checkIn, checkOut);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }
}
