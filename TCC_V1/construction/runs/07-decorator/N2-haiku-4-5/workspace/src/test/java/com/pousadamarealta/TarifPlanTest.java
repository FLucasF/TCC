package com.pousadamarealta;

import static org.junit.jupiter.api.Assertions.*;

import com.pousadamarealta.tariff.NonRefundableTarifPlan;
import com.pousadamarealta.tariff.StandardTarifPlan;
import com.pousadamarealta.tariff.TarifPlan;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TarifPlanTest {
    @Test
    void testStandardTarifHighSeason() {
        TarifPlan plan = new StandardTarifPlan();
        BigDecimal nightlyRate = new BigDecimal("249.90");
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        BigDecimal subtotal = plan.calculateSubtotal(nightlyRate, 2, checkIn);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testStandardTarifDiscount() {
        TarifPlan plan = new StandardTarifPlan();
        BigDecimal discount = plan.calculateDiscount(new BigDecimal("100.00"));
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void testStandardTarifRefund() {
        TarifPlan plan = new StandardTarifPlan();
        BigDecimal refund = plan.calculateRefund(new BigDecimal("500.00"));
        assertEquals(new BigDecimal("500.00"), refund);
    }

    @Test
    void testNonRefundableTarifDiscount() {
        TarifPlan plan = new NonRefundableTarifPlan();
        BigDecimal discount = plan.calculateDiscount(new BigDecimal("100.00"));
        assertEquals(new BigDecimal("15.00"), discount);
    }

    @Test
    void testNonRefundableTarifRefund() {
        TarifPlan plan = new NonRefundableTarifPlan();
        BigDecimal refund = plan.calculateRefund(new BigDecimal("500.00"));
        assertEquals(BigDecimal.ZERO, refund);
    }
}
