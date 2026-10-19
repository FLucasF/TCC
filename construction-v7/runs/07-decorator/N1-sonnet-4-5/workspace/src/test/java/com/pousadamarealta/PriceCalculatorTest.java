package com.pousadamarealta;

import com.pousadamarealta.domain.RatePlan;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.util.PriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PriceCalculatorTest {

    @Test
    public void testLowSeasonPrice() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate night = LocalDate.of(2027, 3, 10);
        BigDecimal price = PriceCalculator.calculateNightPrice(room, night);
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    public void testHighSeasonPriceJuly() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate night = LocalDate.of(2027, 7, 10);
        BigDecimal price = PriceCalculator.calculateNightPrice(room, night);
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testHighSeasonPriceDecember() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate night = LocalDate.of(2027, 12, 20);
        BigDecimal price = PriceCalculator.calculateNightPrice(room, night);
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    public void testSubtotalHighSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    public void testNonRefundableDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    public void testStandardPlanNoDiscount() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    public void testTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }
}
