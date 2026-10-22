package com.pousadamarealta.util;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorTest {

    @Test
    void shouldCalculateRegularSeasonPrice() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = PriceCalculator.calculateRegularSubtotal(room, checkIn, checkOut);

        assertEquals(new BigDecimal("499.80"), subtotal);
    }

    @Test
    void shouldCalculateHighSeasonPrice() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PriceCalculator.calculateRegularSubtotal(room, checkIn, checkOut);

        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void shouldCalculateNights() {
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        long nights = PriceCalculator.calculateNights(checkIn, checkOut);

        assertEquals(2, nights);
    }
}
