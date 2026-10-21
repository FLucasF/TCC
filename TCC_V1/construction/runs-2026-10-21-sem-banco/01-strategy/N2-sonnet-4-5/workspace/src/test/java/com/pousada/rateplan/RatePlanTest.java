package com.pousada.rateplan;

import com.pousada.domain.RoomType;
import com.pousada.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class RatePlanTest {

    @Test
    void testStandardPlan() {
        StandardRatePlan plan = new StandardRatePlan();
        PricingContext context = new PricingContext(
            RoomType.STANDARD,
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            2,
            null
        );

        PricingResult result = plan.calculatePrice(context);

        assertEquals(new BigDecimal("599.76"), result.getSubtotal());
        assertEquals(BigDecimal.ZERO, result.getDiscount());
        assertEquals(new BigDecimal("599.76"), result.getTotal());
    }

    @Test
    void testNonRefundablePlan() {
        NonRefundableRatePlan plan = new NonRefundableRatePlan();
        PricingContext context = new PricingContext(
            RoomType.DELUXE,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            null
        );

        PricingResult result = plan.calculatePrice(context);

        assertEquals(new BigDecimal("779.80"), result.getSubtotal());
        assertEquals(new BigDecimal("116.97"), result.getDiscount());
        assertEquals(new BigDecimal("662.83"), result.getTotal());
    }

    @Test
    void testCorporatePlanTecnosul() {
        CorporateRatePlan plan = new CorporateRatePlan();
        PricingContext context = new PricingContext(
            RoomType.DELUXE,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "TECNOSUL"
        );

        PricingResult result = plan.calculatePrice(context);

        assertEquals(new BigDecimal("779.80"), result.getSubtotal());
        assertEquals(new BigDecimal("93.58"), result.getDiscount());
        assertEquals(new BigDecimal("686.22"), result.getTotal());
    }

    @Test
    void testCorporatePlanInvalidCode() {
        CorporateRatePlan plan = new CorporateRatePlan();
        PricingContext context = new PricingContext(
            RoomType.DELUXE,
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            2,
            "INVALID"
        );

        assertThrows(BusinessException.class, () -> plan.validate(context));
    }

    @Test
    void testWeekendPackageDeluxe() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();
        PricingContext context = new PricingContext(
            RoomType.DELUXE,
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14),
            2,
            null
        );

        PricingResult result = plan.calculatePrice(context);

        assertEquals(new BigDecimal("820.00"), result.getSubtotal());
        assertEquals(BigDecimal.ZERO, result.getDiscount());
        assertEquals(new BigDecimal("820.00"), result.getTotal());
    }

    @Test
    void testWeekendPackageRefund() {
        WeekendPackageRatePlan plan = new WeekendPackageRatePlan();
        BigDecimal refund = plan.calculateRefund(new BigDecimal("820.00"));
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    void testLongStayPlan() {
        LongStayRatePlan plan = new LongStayRatePlan();
        PricingContext context = new PricingContext(
            RoomType.STANDARD,
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 3, 8),
            2,
            null
        );

        PricingResult result = plan.calculatePrice(context);

        assertEquals(new BigDecimal("1749.30"), result.getSubtotal());
        assertEquals(new BigDecimal("174.93"), result.getDiscount());
        assertEquals(new BigDecimal("1574.37"), result.getTotal());
    }

    @Test
    void testLongStayInvalidNights() {
        LongStayRatePlan plan = new LongStayRatePlan();
        PricingContext context = new PricingContext(
            RoomType.STANDARD,
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 3, 7),
            2,
            null
        );

        assertThrows(BusinessException.class, () -> plan.validate(context));
    }
}
