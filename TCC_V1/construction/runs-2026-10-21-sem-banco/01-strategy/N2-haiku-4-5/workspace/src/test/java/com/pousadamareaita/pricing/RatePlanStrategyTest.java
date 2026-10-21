package com.pousadamareaita.pricing;

import com.pousadamareaita.model.RoomType;
import org.junit.Test;
import static org.junit.Assert.*;
import java.time.LocalDate;
import java.math.BigDecimal;

public class RatePlanStrategyTest {
    private void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertEquals(0, expected.compareTo(actual));
    }

    @Test
    public void testStandardPlan() {
        StandardPlanStrategy strategy = new StandardPlanStrategy();
        PriceCalculation calc = strategy.calculatePrice(
            LocalDate.of(2027, 7, 10),
            LocalDate.of(2027, 7, 12),
            BigDecimal.valueOf(249.90),
            2
        );

        assertEquals(2, calc.getNights());
        assertBigDecimalEquals(new BigDecimal("599.76"), calc.getSubtotal());
        assertBigDecimalEquals(BigDecimal.ZERO, calc.getDiscount());
        assertBigDecimalEquals(new BigDecimal("599.76"), calc.getTotal());
    }

    @Test
    public void testNonRefundablePlan() {
        NonRefundablePlanStrategy strategy = new NonRefundablePlanStrategy();
        PriceCalculation calc = strategy.calculatePrice(
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            BigDecimal.valueOf(389.90),
            2
        );

        assertEquals(2, calc.getNights());
        assertBigDecimalEquals(new BigDecimal("779.80"), calc.getSubtotal());
        assertBigDecimalEquals(new BigDecimal("116.97"), calc.getDiscount());
        assertBigDecimalEquals(new BigDecimal("662.83"), calc.getTotal());
    }

    @Test
    public void testCorporatePlanTecnosul() {
        CorporatePlanStrategy strategy = new CorporatePlanStrategy("TECNOSUL");
        PriceCalculation calc = strategy.calculatePrice(
            LocalDate.of(2027, 3, 10),
            LocalDate.of(2027, 3, 12),
            BigDecimal.valueOf(389.90),
            2
        );

        assertEquals(2, calc.getNights());
        assertBigDecimalEquals(new BigDecimal("779.80"), calc.getSubtotal());
        assertBigDecimalEquals(new BigDecimal("93.58"), calc.getDiscount());
        assertBigDecimalEquals(new BigDecimal("686.22"), calc.getTotal());
    }

    @Test
    public void testWeekendPackagePlan() {
        WeekendPackagePlanStrategy strategy = new WeekendPackagePlanStrategy(RoomType.DELUXE);
        PriceCalculation calc = strategy.calculatePrice(
            LocalDate.of(2027, 3, 12),
            LocalDate.of(2027, 3, 14),
            BigDecimal.valueOf(389.90),
            2
        );

        assertEquals(2, calc.getNights());
        assertBigDecimalEquals(new BigDecimal("820.00"), calc.getSubtotal());
        assertBigDecimalEquals(BigDecimal.ZERO, calc.getDiscount());
        assertBigDecimalEquals(new BigDecimal("820.00"), calc.getTotal());
    }

    @Test
    public void testLongStayPlan() {
        LongStayPlanStrategy strategy = new LongStayPlanStrategy();
        PriceCalculation calc = strategy.calculatePrice(
            LocalDate.of(2027, 3, 1),
            LocalDate.of(2027, 3, 8),
            BigDecimal.valueOf(249.90),
            1
        );

        assertEquals(7, calc.getNights());
        assertBigDecimalEquals(new BigDecimal("1749.30"), calc.getSubtotal());
        assertBigDecimalEquals(new BigDecimal("174.93"), calc.getDiscount());
        assertBigDecimalEquals(new BigDecimal("1574.37"), calc.getTotal());
    }
}
