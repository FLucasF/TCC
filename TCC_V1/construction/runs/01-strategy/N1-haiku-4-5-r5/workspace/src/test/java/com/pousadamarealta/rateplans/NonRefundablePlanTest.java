package com.pousadamarealta.rateplans;

import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class NonRefundablePlanTest {
    private NonRefundablePlan plan = new NonRefundablePlan();

    @Test
    public void testNonRefundableDiscount() {
        Room room = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        RatePlan.PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals("779.80", String.format("%.2f", calc.subtotal));
        assertEquals("116.97", String.format("%.2f", calc.discount));
        assertEquals("662.83", String.format("%.2f", calc.total));
    }
}
