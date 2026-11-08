package com.pousadamarealta.rateplans;

import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class StandardPlanTest {
    private StandardPlan plan = new StandardPlan();

    @Test
    public void testStandardPlanLowSeason() {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        RatePlan.PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals("499.80", String.format("%.2f", calc.subtotal));
        assertEquals("0.00", String.format("%.2f", calc.discount));
        assertEquals("499.80", String.format("%.2f", calc.total));
    }

    @Test
    public void testStandardPlanHighSeason() {
        Room room = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        RatePlan.PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals("599.76", String.format("%.2f", calc.subtotal));
        assertEquals("0.00", String.format("%.2f", calc.discount));
        assertEquals("599.76", String.format("%.2f", calc.total));
    }
}
