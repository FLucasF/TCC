package com.pousadamarealta.rateplans;

import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class CorporatePlanTest {
    private CorporatePlan plan = new CorporatePlan();

    @Test
    public void testCorporateTecnosul() {
        Room room = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        RatePlan.PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut, "TECNOSUL");

        assertEquals("779.80", String.format("%.2f", calc.subtotal));
        assertEquals("93.58", String.format("%.2f", calc.discount));
        assertEquals("686.22", String.format("%.2f", calc.total));
    }

    @Test
    public void testInvalidCompanyCode() throws RatePlan.InvalidRatePlanException {
        Room room = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        try {
            plan.validateAppliesTo(room, checkIn, checkOut, 2, "INVALID");
            fail("Should throw exception");
        } catch (RatePlan.InvalidRatePlanException e) {
            assertTrue(true);
        }
    }
}
