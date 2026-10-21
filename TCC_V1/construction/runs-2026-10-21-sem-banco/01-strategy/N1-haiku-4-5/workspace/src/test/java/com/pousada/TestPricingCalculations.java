package com.pousada;

import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.domain.rateplans.*;
import com.pousada.util.Money;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class TestPricingCalculations {

    @Test
    public void testStandardPlan() {
        Room room = new Room(101, RoomType.STANDARD);
        StandardPlan plan = new StandardPlan();

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        RatePlan.PriceCalculation prices = plan.calculatePrices(room, checkIn, checkOut);

        assertEquals(599.76, prices.subtotal, 0.01);
        assertEquals(0.0, prices.discount, 0.01);
        assertEquals(599.76, prices.total, 0.01);
    }

    @Test
    public void testNonRefundablePlan() {
        Room room = new Room(101, RoomType.STANDARD);
        NonRefundablePlan plan = new NonRefundablePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        RatePlan.PriceCalculation prices = plan.calculatePrices(room, checkIn, checkOut);

        assertEquals(499.80, prices.subtotal, 0.01);
        assertEquals(74.97, prices.discount, 0.01);
        assertEquals(424.83, prices.total, 0.01);
    }

    @Test
    public void testCorporatePlanTecnosul() {
        Room room = new Room(201, RoomType.DELUXE);
        CorporatePlan plan = new CorporatePlan("TECNOSUL");

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        RatePlan.PriceCalculation prices = plan.calculatePrices(room, checkIn, checkOut);

        assertEquals(779.80, prices.subtotal, 0.01);
        assertEquals(93.58, prices.discount, 0.01);
        assertEquals(686.22, prices.total, 0.01);
    }

    @Test
    public void testWeekendPackage() {
        Room room = new Room(201, RoomType.DELUXE);
        WeekendPackagePlan plan = new WeekendPackagePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        RatePlan.PriceCalculation prices = plan.calculatePrices(room, checkIn, checkOut);

        assertEquals(820.00, prices.subtotal, 0.01);
        assertEquals(0.0, prices.discount, 0.01);
        assertEquals(820.00, prices.total, 0.01);
    }

    @Test
    public void testWeekendPackageRefund() {
        WeekendPackagePlan plan = new WeekendPackagePlan();
        double refund = plan.calculateRefund(820.00);
        assertEquals(410.00, refund, 0.01);
    }

    @Test
    public void testLongStayPlan() {
        Room room = new Room(101, RoomType.STANDARD);
        LongStayPlan plan = new LongStayPlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        RatePlan.PriceCalculation prices = plan.calculatePrices(room, checkIn, checkOut);

        assertEquals(1749.30, prices.subtotal, 0.01);
        assertEquals(174.93, prices.discount, 0.01);
        assertEquals(1574.37, prices.total, 0.01);
    }
}
