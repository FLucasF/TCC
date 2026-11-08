package com.marealta.pousada;

import com.marealta.pousada.domain.model.RoomType;
import com.marealta.pousada.domain.rateplan.*;
import com.marealta.pousada.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class RatePlanTest {

    @Test
    public void testStandardRatePlan() {
        RatePlan plan = new StandardRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, RoomType.DELUXE);
        BigDecimal discount = plan.calculateDiscount(subtotal);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(BigDecimal.ZERO, discount);
        assertEquals(subtotal, plan.calculateRefund(subtotal));
    }

    @Test
    public void testNonRefundableRatePlan() {
        RatePlan plan = new NonRefundableRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, RoomType.DELUXE);
        BigDecimal discount = plan.calculateDiscount(subtotal);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(new BigDecimal("116.97"), discount);
        assertEquals(BigDecimal.ZERO, plan.calculateRefund(new BigDecimal("662.83")));
    }

    @Test
    public void testCorporateRatePlanTecnosul() {
        RatePlan plan = new CorporateRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        plan.validate(checkIn, checkOut, RoomType.DELUXE, "TECNOSUL");

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, RoomType.DELUXE);
        BigDecimal discount = plan.calculateDiscount(subtotal);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(new BigDecimal("93.58"), discount);
    }

    @Test
    public void testCorporateRatePlanInvalidCode() {
        RatePlan plan = new CorporateRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(InvalidRequestException.class, () -> {
            plan.validate(checkIn, checkOut, RoomType.DELUXE, "INVALID");
        });
    }

    @Test
    public void testWeekendPackageRatePlan() {
        RatePlan plan = new WeekendPackageRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 12); // Friday
        LocalDate checkOut = LocalDate.of(2027, 3, 14); // Sunday

        plan.validate(checkIn, checkOut, RoomType.DELUXE, null);

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, RoomType.DELUXE);
        BigDecimal discount = plan.calculateDiscount(subtotal);

        assertEquals(new BigDecimal("820.00"), subtotal);
        assertEquals(BigDecimal.ZERO, discount);

        BigDecimal refund = plan.calculateRefund(new BigDecimal("820.00"));
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    public void testWeekendPackageInvalidDates() {
        RatePlan plan = new WeekendPackageRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10); // Monday
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(InvalidRequestException.class, () -> {
            plan.validate(checkIn, checkOut, RoomType.DELUXE, null);
        });
    }

    @Test
    public void testLongStayRatePlan() {
        RatePlan plan = new LongStayRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        plan.validate(checkIn, checkOut, RoomType.STANDARD, null);

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, RoomType.STANDARD);
        BigDecimal discount = plan.calculateDiscount(subtotal);

        assertEquals(new BigDecimal("1749.30"), subtotal);
        assertEquals(new BigDecimal("174.93"), discount);
    }

    @Test
    public void testLongStayInvalidDuration() {
        RatePlan plan = new LongStayRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 4); // Only 3 nights

        assertThrows(InvalidRequestException.class, () -> {
            plan.validate(checkIn, checkOut, RoomType.STANDARD, null);
        });
    }

    @Test
    public void testHighSeasonCalculation() {
        RatePlan plan = new StandardRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 7, 10); // July = high season
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, RoomType.STANDARD);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }
}
