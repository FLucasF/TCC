package com.pousadamarealta.pricing;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PricingTest {
    private static final BigDecimal STANDARD_RATE = new BigDecimal("249.90");
    private static final BigDecimal DELUXE_RATE = new BigDecimal("389.90");

    @Test
    public void testStandardPlanOffSeason() {
        Room room = new Room(101, RoomType.STANDARD, 2, STANDARD_RATE);
        RatePlan plan = new StandardPlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(new BigDecimal("499.80"), calc.subtotal());
        assertEquals(BigDecimal.ZERO, calc.discount());
        assertEquals(new BigDecimal("499.80"), calc.total());
    }

    @Test
    public void testStandardPlanHighSeason() {
        Room room = new Room(101, RoomType.STANDARD, 2, STANDARD_RATE);
        RatePlan plan = new StandardPlan();

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(new BigDecimal("599.76"), calc.subtotal());
        assertEquals(BigDecimal.ZERO, calc.discount());
        assertEquals(new BigDecimal("599.76"), calc.total());
    }

    @Test
    public void testNonRefundablePlan() {
        Room room = new Room(101, RoomType.STANDARD, 2, STANDARD_RATE);
        RatePlan plan = new NonRefundablePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(new BigDecimal("499.80"), calc.subtotal());
        assertEquals(new BigDecimal("74.97"), calc.discount());
        assertEquals(new BigDecimal("424.83"), calc.total());
    }

    @Test
    public void testCorporatePlanTecnosul() {
        Room room = new Room(201, RoomType.DELUXE, 3, DELUXE_RATE);
        RatePlan plan = new CorporatePlan();
        plan.validate(201, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), 2, "TECNOSUL");

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(new BigDecimal("779.80"), calc.subtotal());
        assertEquals(new BigDecimal("93.58"), calc.discount());
        assertEquals(new BigDecimal("686.22"), calc.total());
    }

    @Test
    public void testWeekendPackageDeluxe() {
        Room room = new Room(201, RoomType.DELUXE, 3, DELUXE_RATE);
        RatePlan plan = new WeekendPackagePlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        plan.validate(201, checkIn, checkOut, 2, null);
        PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(0, MoneyRounder.round(new BigDecimal("820.00")).compareTo(calc.subtotal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(calc.discount()));
        assertEquals(0, MoneyRounder.round(new BigDecimal("820.00")).compareTo(calc.total()));

        BigDecimal refund = plan.calculateRefund(new BigDecimal("820.00"));
        assertEquals(0, MoneyRounder.round(new BigDecimal("410.00")).compareTo(refund));
    }

    @Test
    public void testLongStayPlan() {
        Room room = new Room(101, RoomType.STANDARD, 2, STANDARD_RATE);
        RatePlan plan = new LongStayPlan();

        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        plan.validate(101, checkIn, checkOut, 1, null);
        PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals(new BigDecimal("1749.30"), calc.subtotal());
        assertEquals(new BigDecimal("174.93"), calc.discount());
        assertEquals(new BigDecimal("1574.37"), calc.total());
    }
}
