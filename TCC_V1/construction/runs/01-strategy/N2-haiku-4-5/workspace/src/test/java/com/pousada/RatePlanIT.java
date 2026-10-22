package com.pousada;

import com.pousada.domain.Room;
import com.pousada.rateplan.*;
import com.pousada.utils.Money;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.Assert.*;

public class RatePlanIT {
    private Room standardRoom;
    private Room deluxeRoom;
    private Room suiteRoom;

    @Before
    public void setUp() {
        standardRoom = new Room(101, Room.Type.STANDARD, 2, BigDecimal.valueOf(249.90));
        deluxeRoom = new Room(201, Room.Type.DELUXE, 3, BigDecimal.valueOf(389.90));
        suiteRoom = new Room(301, Room.Type.SUITE, 4, BigDecimal.valueOf(579.90));
    }

    @Test
    public void testStandardPlan() throws RatePlan.RatePlanException {
        StandardPlan plan = new StandardPlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        plan.validate(deluxeRoom, checkIn, checkOut, 2, null);
        RatePlan.PriceCalculation calc = plan.calculatePrice(deluxeRoom, checkIn, checkOut);

        assertEquals(0, calc.subtotal().compareTo(BigDecimal.valueOf(779.80)));
        assertEquals(0, calc.discount().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testNonRefundablePlan() throws RatePlan.RatePlanException {
        NonRefundablePlan plan = new NonRefundablePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        plan.validate(deluxeRoom, checkIn, checkOut, 2, null);
        RatePlan.PriceCalculation calc = plan.calculatePrice(deluxeRoom, checkIn, checkOut);

        assertEquals(0, calc.subtotal().compareTo(BigDecimal.valueOf(779.80)));
        assertTrue(calc.discount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    public void testCorporatePlan() throws RatePlan.RatePlanException {
        CorporatePlan plan = new CorporatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        plan.validate(deluxeRoom, checkIn, checkOut, 2, "TECNOSUL");
        RatePlan.PriceCalculation calc = plan.calculatePrice(deluxeRoom, checkIn, checkOut);

        assertEquals(0, calc.subtotal().compareTo(BigDecimal.valueOf(779.80)));

        BigDecimal discount = Money.percentage(calc.subtotal(), plan.getCompanyDiscount("TECNOSUL"));
        assertEquals(0, discount.compareTo(BigDecimal.valueOf(93.58)));
    }

    @Test
    public void testCorporatePlanInvalidCode() {
        CorporatePlan plan = new CorporatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(RatePlan.RatePlanException.class,
            () -> plan.validate(deluxeRoom, checkIn, checkOut, 2, "INVALID"));
    }

    @Test
    public void testWeekendPackagePlan() throws RatePlan.RatePlanException {
        WeekendPackagePlan plan = new WeekendPackagePlan();
        LocalDate friday = LocalDate.of(2027, 3, 12);
        LocalDate sunday = LocalDate.of(2027, 3, 14);

        plan.validate(deluxeRoom, friday, sunday, 2, null);
        RatePlan.PriceCalculation calc = plan.calculatePrice(deluxeRoom, friday, sunday);

        assertEquals(0, calc.subtotal().compareTo(BigDecimal.valueOf(820.00)));
        assertEquals(0, calc.discount().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testLongStayPlan() throws RatePlan.RatePlanException {
        LongStayPlan plan = new LongStayPlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        plan.validate(standardRoom, checkIn, checkOut, 1, null);
        RatePlan.PriceCalculation calc = plan.calculatePrice(standardRoom, checkIn, checkOut);

        assertEquals(0, calc.subtotal().compareTo(BigDecimal.valueOf(1749.30)));
        assertEquals(0, calc.discount().compareTo(BigDecimal.valueOf(174.93)));
    }

    @Test
    public void testLongStayPlanMinNights() {
        LongStayPlan plan = new LongStayPlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 7);

        assertThrows(RatePlan.RatePlanException.class,
            () -> plan.validate(standardRoom, checkIn, checkOut, 1, null));
    }
}
