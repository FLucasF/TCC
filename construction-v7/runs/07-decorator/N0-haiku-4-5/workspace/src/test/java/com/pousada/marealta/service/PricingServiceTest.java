package com.pousada.marealta.service;

import com.pousada.marealta.model.RatePlan;
import com.pousada.marealta.model.Room;
import com.pousada.marealta.model.RoomType;
import com.pousada.marealta.util.MoneyUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {
    @Test
    void testStandardRoomStandardPlan() {
        Room room = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        PricingService.PriceCalculation calc = PricingService.calculate(room, checkIn, checkOut, RatePlan.STANDARD);

        assertEquals(2, calc.nights);
        assertEquals(new BigDecimal("599.76"), calc.subtotal);
        assertEquals(BigDecimal.ZERO, calc.discount);
        assertEquals(new BigDecimal("599.76"), calc.total);
    }

    @Test
    void testNonRefundableDiscount() {
        Room room = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        PricingService.PriceCalculation calc = PricingService.calculate(room, checkIn, checkOut, RatePlan.NON_REFUNDABLE);

        assertEquals(2, calc.nights);
        assertEquals(new BigDecimal("779.80"), calc.subtotal);
        assertEquals(new BigDecimal("116.97"), calc.discount);
        assertEquals(new BigDecimal("662.83"), calc.total);
    }
}
