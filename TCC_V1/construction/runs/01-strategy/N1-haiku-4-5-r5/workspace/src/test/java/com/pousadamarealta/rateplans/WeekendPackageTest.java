package com.pousadamarealta.rateplans;

import com.pousadamarealta.models.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.Test;
import static org.junit.Assert.*;

public class WeekendPackageTest {
    private WeekendPackagePlan plan = new WeekendPackagePlan();

    @Test
    public void testWeekendPackageDeluxe() throws RatePlan.InvalidRatePlanException {
        Room room = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        plan.validateAppliesTo(room, checkIn, checkOut, 2, null);
        RatePlan.PriceCalculation calc = plan.calculatePrice(room, checkIn, checkOut);

        assertEquals("820.00", String.format("%.2f", calc.subtotal));
        assertEquals("0.00", String.format("%.2f", calc.discount));
        assertEquals("820.00", String.format("%.2f", calc.total));
    }

    @Test
    public void testWeekendPackageRefund() {
        BigDecimal refund = plan.getRefundAmount(new BigDecimal("820.00"));
        assertEquals("410.00", String.format("%.2f", refund));
    }
}
