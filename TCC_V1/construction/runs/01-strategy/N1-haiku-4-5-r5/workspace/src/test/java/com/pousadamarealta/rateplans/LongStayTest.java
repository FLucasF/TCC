package com.pousadamarealta.rateplans;

import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class LongStayTest {
    private LongStayPlan plan = new LongStayPlan();

    @Test
    public void testLongStayStandard() throws RatePlan.InvalidRatePlanException {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        plan.validateAppliesTo(room, checkIn, checkOut, 2, null);
        RatePlan.PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals("1749.30", String.format("%.2f", calc.subtotal));
        assertEquals("174.93", String.format("%.2f", calc.discount));
        assertEquals("1574.37", String.format("%.2f", calc.total));
    }

    @Test
    public void testLongStayMinimumNights() throws RatePlan.InvalidRatePlanException {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 7);

        try {
            plan.validateAppliesTo(room, checkIn, checkOut, 2, null);
            fail("Should require 7 nights");
        } catch (RatePlan.InvalidRatePlanException e) {
            assertTrue(true);
        }
    }
}
