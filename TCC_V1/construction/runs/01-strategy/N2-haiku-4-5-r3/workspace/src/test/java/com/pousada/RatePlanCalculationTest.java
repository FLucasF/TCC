package com.pousada;

import com.pousada.rateplan.*;
import com.pousada.util.Money;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class RatePlanCalculationTest {
    private BigDecimal standardRate = new BigDecimal("249.90");
    private BigDecimal deluxeRate = new BigDecimal("389.90");
    private LocalDate checkIn = LocalDate.of(2027, 7, 10);
    private LocalDate checkOut = LocalDate.of(2027, 7, 12);
    private Map<String, Object> context;

    @Before
    public void setUp() {
        context = new HashMap<>();
    }

    @Test
    public void testStandardPlan() throws RatePlanException {
        // July 10-12, 2027 is high season
        // 2 nights at 249.90 * 1.20 = 299.88 each
        // Total: 599.76
        RatePlan plan = new StandardRatePlan();
        plan.validate(2, 2, checkIn, checkOut, 1, context);

        RatePlan.PriceCalculation calc = plan.calculate(standardRate, 2, checkIn, checkOut, context);

        assertEquals(new BigDecimal("599.76"), calc.subtotal);
        assertEquals(BigDecimal.ZERO, calc.discount);
        assertEquals(new BigDecimal("599.76"), calc.total);
    }

    @Test
    public void testNonRefundablePlan() throws RatePlanException {
        // Same as standard but with 15% discount
        RatePlan plan = new NonRefundableRatePlan();
        plan.validate(2, 2, checkIn, checkOut, 1, context);

        RatePlan.PriceCalculation calc = plan.calculate(standardRate, 2, checkIn, checkOut, context);

        assertEquals(new BigDecimal("599.76"), calc.subtotal);
        assertEquals(new BigDecimal("89.96"), calc.discount);
        assertEquals(new BigDecimal("509.80"), calc.total);
    }

    @Test
    public void testCorporatePlanTecnosul() throws RatePlanException {
        // TECNOSUL: 12% discount on deluxe 10/03-12/03/2027
        // March is not high season
        // 2 nights at 389.90 = 779.80
        // 12% discount = 93.58
        // Total: 686.22
        context.put("companyCode", "TECNOSUL");
        LocalDate marchCheckIn = LocalDate.of(2027, 3, 10);
        LocalDate marchCheckOut = LocalDate.of(2027, 3, 12);

        RatePlan plan = new CorporateRatePlan();
        plan.validate(3, 2, marchCheckIn, marchCheckOut, 1, context);

        RatePlan.PriceCalculation calc = plan.calculate(deluxeRate, 2, marchCheckIn, marchCheckOut, context);

        assertEquals(new BigDecimal("779.80"), calc.subtotal);
        assertEquals(new BigDecimal("93.58"), calc.discount);
        assertEquals(new BigDecimal("686.22"), calc.total);
    }

    @Test
    public void testWeekendPackage() throws RatePlanException {
        // March 12-14, 2027 is Friday-Sunday
        context.put("roomType", "DELUXE");
        LocalDate friday = LocalDate.of(2027, 3, 12);
        LocalDate sunday = LocalDate.of(2027, 3, 14);

        RatePlan plan = new WeekendPackageRatePlan();
        plan.validate(3, 2, friday, sunday, 1, context);

        RatePlan.PriceCalculation calc = plan.calculate(deluxeRate, 2, friday, sunday, context);

        assertEquals(new BigDecimal("820.00"), calc.subtotal);
        assertEquals(BigDecimal.ZERO, calc.discount);
        assertEquals(new BigDecimal("820.00"), calc.total);
    }

    @Test
    public void testLongStay() throws RatePlanException {
        // March 1-8, 2027 is 7 nights (not high season)
        // 7 nights at 249.90 = 1749.30
        // 10% discount = 174.93
        // Total: 1574.37
        LocalDate longStayCheckIn = LocalDate.of(2027, 3, 1);
        LocalDate longStayCheckOut = LocalDate.of(2027, 3, 8);

        RatePlan plan = new LongStayRatePlan();
        plan.validate(2, 7, longStayCheckIn, longStayCheckOut, 1, context);

        RatePlan.PriceCalculation calc = plan.calculate(standardRate, 7, longStayCheckIn, longStayCheckOut, context);

        assertEquals(new BigDecimal("1749.30"), calc.subtotal);
        assertEquals(new BigDecimal("174.93"), calc.discount);
        assertEquals(new BigDecimal("1574.37"), calc.total);
    }

    @Test
    public void testWeekendPackageRefund() {
        RatePlan plan = new WeekendPackageRatePlan();
        BigDecimal refund = plan.calculateRefund(new BigDecimal("820.00"));
        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    public void testStandardPlanRefund() {
        RatePlan plan = new StandardRatePlan();
        BigDecimal refund = plan.calculateRefund(new BigDecimal("500.00"));
        assertEquals(new BigDecimal("500.00"), refund);
    }

    @Test
    public void testNonRefundableRefund() {
        RatePlan plan = new NonRefundableRatePlan();
        BigDecimal refund = plan.calculateRefund(new BigDecimal("500.00"));
        assertEquals(BigDecimal.ZERO, refund);
    }
}
