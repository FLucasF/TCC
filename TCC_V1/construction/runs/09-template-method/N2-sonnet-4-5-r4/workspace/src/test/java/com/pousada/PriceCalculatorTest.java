package com.pousada;

import com.pousada.domain.RatePlan;
import com.pousada.domain.Room;
import com.pousada.domain.RoomType;
import com.pousada.service.PriceCalculator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceCalculatorTest {

    @Test
    void shouldCalculateStandardPriceInLowSeason() {
        PriceCalculator calculator = new PriceCalculator();
        Room room = new Room(101, RoomType.STANDARD);

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = calculator.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = calculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        BigDecimal total = calculator.calculateTotal(subtotal, discount);

        assertEquals(0, new BigDecimal("499.80").compareTo(subtotal));
        assertEquals(0, BigDecimal.ZERO.compareTo(discount));
        assertEquals(0, new BigDecimal("499.80").compareTo(total));
    }

    @Test
    void shouldCalculateHighSeasonPrice() {
        PriceCalculator calculator = new PriceCalculator();
        Room room = new Room(101, RoomType.STANDARD);

        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = calculator.calculateSubtotal(room, checkIn, checkOut);

        assertEquals(0, new BigDecimal("599.76").compareTo(subtotal));
    }

    @Test
    void shouldApplyNonRefundableDiscount() {
        PriceCalculator calculator = new PriceCalculator();
        Room room = new Room(101, RoomType.STANDARD);

        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = calculator.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = calculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        BigDecimal total = calculator.calculateTotal(subtotal, discount);

        assertEquals(0, new BigDecimal("499.80").compareTo(subtotal));
        assertEquals(0, new BigDecimal("74.97").compareTo(discount));
        assertEquals(0, new BigDecimal("424.83").compareTo(total));
    }

    @Test
    void shouldCalculateDecemberHighSeason() {
        PriceCalculator calculator = new PriceCalculator();
        Room room = new Room(201, RoomType.DELUXE);

        LocalDate checkIn = LocalDate.of(2027, 12, 15);
        LocalDate checkOut = LocalDate.of(2027, 12, 16);

        BigDecimal subtotal = calculator.calculateSubtotal(room, checkIn, checkOut);

        BigDecimal expected = new BigDecimal("389.90").multiply(new BigDecimal("1.20"))
            .setScale(2, java.math.RoundingMode.HALF_EVEN);
        assertEquals(0, expected.compareTo(subtotal));
    }

    @Test
    void shouldNotApplyHighSeasonBeforeDecember15() {
        PriceCalculator calculator = new PriceCalculator();
        Room room = new Room(201, RoomType.DELUXE);

        LocalDate checkIn = LocalDate.of(2027, 12, 14);
        LocalDate checkOut = LocalDate.of(2027, 12, 15);

        BigDecimal subtotal = calculator.calculateSubtotal(room, checkIn, checkOut);

        assertEquals(0, new BigDecimal("389.90").compareTo(subtotal));
    }
}
