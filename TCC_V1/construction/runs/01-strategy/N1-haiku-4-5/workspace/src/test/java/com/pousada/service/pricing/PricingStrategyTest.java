package com.pousada.service.pricing;

import com.pousada.model.Room;
import com.pousada.model.RoomType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingStrategyTest {
    private final Room deluxe = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));
    private final Room standard = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));

    @Test
    void testCorporateTecnosulExample() {
        PricingStrategy strategy = new CorporatePricingStrategy("TECNOSUL");
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);
        PricingResult result = strategy.calculatePrice(deluxe, checkIn, checkOut, 2);

        assertEquals(0, result.getSubtotal().compareTo(new BigDecimal("779.80")));
        assertEquals(0, result.getDiscount().compareTo(new BigDecimal("93.58")));
        assertEquals(0, result.getTotal().compareTo(new BigDecimal("686.22")));
    }

    @Test
    void testWeekendPackageDeluxeExample() {
        PricingStrategy strategy = new WeekendPackagePricingStrategy();
        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);
        PricingResult result = strategy.calculatePrice(deluxe, checkIn, checkOut, 2);

        assertEquals(0, result.getTotal().compareTo(new BigDecimal("820.00")));
    }

    @Test
    void testLongStayStandardExample() {
        PricingStrategy strategy = new LongStayPricingStrategy();
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);
        PricingResult result = strategy.calculatePrice(standard, checkIn, checkOut, 7);

        assertEquals(0, result.getSubtotal().compareTo(new BigDecimal("1749.30")));
        assertEquals(0, result.getDiscount().compareTo(new BigDecimal("174.93")));
        assertEquals(0, result.getTotal().compareTo(new BigDecimal("1574.37")));
    }

    @Test
    void testWeekendPackageRefund() {
        RefundPolicy policy = new WeekendPackagePricingStrategy().getRefundPolicy();
        BigDecimal refund = policy.calculateRefund(new BigDecimal("820.00"));
        assertEquals(0, refund.compareTo(new BigDecimal("410.00")));
    }
}
