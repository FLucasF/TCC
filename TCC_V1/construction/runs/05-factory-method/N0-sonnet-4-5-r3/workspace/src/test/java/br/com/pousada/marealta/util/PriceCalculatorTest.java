package br.com.pousada.marealta.util;

import br.com.pousada.marealta.model.RatePlan;
import br.com.pousada.marealta.model.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PriceCalculatorTest {

    @Test
    void shouldIdentifyHighSeasonInJuly() {
        LocalDate julyDate = LocalDate.of(2027, 7, 10);
        assertTrue(PriceCalculator.isHighSeason(julyDate));
    }

    @Test
    void shouldIdentifyHighSeasonInDecember() {
        LocalDate decDate = LocalDate.of(2027, 12, 15);
        assertTrue(PriceCalculator.isHighSeason(decDate));

        LocalDate beforeHighSeason = LocalDate.of(2027, 12, 14);
        assertFalse(PriceCalculator.isHighSeason(beforeHighSeason));
    }

    @Test
    void shouldIdentifyHighSeasonInJanuaryAndFebruary() {
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 1, 1)));
        assertTrue(PriceCalculator.isHighSeason(LocalDate.of(2027, 2, 28)));
    }

    @Test
    void shouldNotIdentifyHighSeasonInMarch() {
        assertFalse(PriceCalculator.isHighSeason(LocalDate.of(2027, 3, 1)));
    }

    @Test
    void shouldCalculateNightPriceWithHighSeason() {
        LocalDate highSeasonDate = LocalDate.of(2027, 7, 10);
        BigDecimal price = PriceCalculator.calculateNightPrice(RoomType.STANDARD, highSeasonDate);
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void shouldCalculateNightPriceWithoutHighSeason() {
        LocalDate normalDate = LocalDate.of(2027, 3, 10);
        BigDecimal price = PriceCalculator.calculateNightPrice(RoomType.STANDARD, normalDate);
        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void shouldCalculateSubtotalForHighSeasonStay() {
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);
        BigDecimal subtotal = PriceCalculator.calculateSubtotal(RoomType.STANDARD, checkIn, checkOut);
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void shouldCalculateDiscountForNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    void shouldNotCalculateDiscountForStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void shouldCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);
        assertEquals(new BigDecimal("509.80"), total);
    }
}
