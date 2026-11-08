package br.com.maralta;

import br.com.maralta.domain.RoomType;
import br.com.maralta.domain.rateplan.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RatePlanTest {

    @Test
    void testStandardRatePlan() {
        StandardRatePlan plan = new StandardRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, 2, RoomType.STANDARD);
        assertEquals(new BigDecimal("599.76"), subtotal);

        BigDecimal discount = plan.calculateDiscount(subtotal);
        assertEquals(BigDecimal.ZERO, discount);

        BigDecimal refund = plan.calculateRefund(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), refund);
    }

    @Test
    void testNonRefundableRatePlan() {
        NonRefundableRatePlan plan = new NonRefundableRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, 2, RoomType.DELUXE);
        assertEquals(new BigDecimal("779.80"), subtotal);

        BigDecimal discount = plan.calculateDiscount(subtotal);
        assertEquals(new BigDecimal("116.97"), discount);

        BigDecimal refund = plan.calculateRefund(new BigDecimal("100.00"));
        assertEquals(BigDecimal.ZERO, refund);
    }

    @Test
    void testCorporateRatePlan() {
        CorporateRatePlan plan = new CorporateRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertDoesNotThrow(() -> plan.validate(checkIn, checkOut, 2, RoomType.DELUXE, "TECNOSUL"));

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, 2, RoomType.DELUXE);
        assertEquals(new BigDecimal("779.80"), subtotal);

        BigDecimal discount = plan.calculateDiscount(subtotal, "TECNOSUL");
        assertEquals(new BigDecimal("93.58"), discount);

        BigDecimal refund = plan.calculateRefund(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), refund);
    }

    @Test
    void testWeekendPackageRatePlan() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        assertDoesNotThrow(() -> plan.validate(checkIn, checkOut, 2, RoomType.DELUXE, null));

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, 2, RoomType.DELUXE);
        assertEquals(new BigDecimal("820.00"), subtotal);

        BigDecimal discount = plan.calculateDiscount(subtotal);
        assertEquals(BigDecimal.ZERO, discount);

        BigDecimal refund = plan.calculateRefund(new BigDecimal("820.00"));
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    void testLongStayRatePlan() {
        LongStayRatePlan plan = new LongStayRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        assertDoesNotThrow(() -> plan.validate(checkIn, checkOut, 7, RoomType.STANDARD, null));

        BigDecimal subtotal = plan.calculateSubtotal(checkIn, checkOut, 7, RoomType.STANDARD);
        assertEquals(new BigDecimal("1749.30"), subtotal);

        BigDecimal discount = plan.calculateDiscount(subtotal);
        assertEquals(new BigDecimal("174.93"), discount);

        BigDecimal refund = plan.calculateRefund(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("100.00"), refund);
    }

    @Test
    void testCorporateRatePlanInvalidCompany() {
        CorporateRatePlan plan = new CorporateRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(IllegalArgumentException.class, () ->
            plan.validate(checkIn, checkOut, 2, RoomType.DELUXE, "INVALID")
        );
    }

    @Test
    void testWeekendPackageInvalidDates() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(IllegalArgumentException.class, () ->
            plan.validate(checkIn, checkOut, 2, RoomType.DELUXE, null)
        );
    }

    @Test
    void testLongStayInvalidNights() {
        LongStayRatePlan plan = new LongStayRatePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 4);

        assertThrows(IllegalArgumentException.class, () ->
            plan.validate(checkIn, checkOut, 3, RoomType.STANDARD, null)
        );
    }
}
