package br.com.pousada;

import br.com.pousada.domain.RatePlan;
import br.com.pousada.domain.Room;
import br.com.pousada.domain.RoomType;
import br.com.pousada.util.PriceCalculator;
import br.com.pousada.util.SeasonUtil;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class PriceCalculatorTest {

    @Test
    void testHighSeasonJuly() {
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 7, 15)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 7, 31)));
    }

    @Test
    void testHighSeasonDecemberToFebruary() {
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2027, 12, 31)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2028, 1, 1)));
        assertTrue(SeasonUtil.isHighSeason(LocalDate.of(2028, 2, 28)));
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2028, 3, 1)));
    }

    @Test
    void testLowSeason() {
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 3, 10)));
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 6, 15)));
        assertFalse(SeasonUtil.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void testCalculateNightPriceHighSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate night = LocalDate.of(2027, 7, 10);

        BigDecimal price = PriceCalculator.calculateNightPrice(room, night);

        // 249.90 * 1.20 = 299.88
        assertEquals(new BigDecimal("299.88"), price);
    }

    @Test
    void testCalculateNightPriceLowSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate night = LocalDate.of(2027, 3, 10);

        BigDecimal price = PriceCalculator.calculateNightPrice(room, night);

        assertEquals(new BigDecimal("249.90"), price);
    }

    @Test
    void testCalculateSubtotalHighSeason() {
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 7, 10);
        LocalDate checkOut = LocalDate.of(2027, 7, 12);

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room, checkIn, checkOut);

        // 2 noites x 299.88 = 599.76
        assertEquals(new BigDecimal("599.76"), subtotal);
    }

    @Test
    void testCalculateDiscountNonRefundable() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.NON_REFUNDABLE);

        // 599.76 * 0.15 = 89.964 -> 89.96 (HALF_EVEN)
        assertEquals(new BigDecimal("89.96"), discount);
    }

    @Test
    void testCalculateDiscountStandard() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);

        assertEquals(new BigDecimal("0.00"), discount);
    }

    @Test
    void testCalculateTotal() {
        BigDecimal subtotal = new BigDecimal("599.76");
        BigDecimal discount = new BigDecimal("89.96");
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        assertEquals(new BigDecimal("509.80"), total);
    }

    @Test
    void testCalculateISS() {
        BigDecimal total = new BigDecimal("779.80");
        BigDecimal iss = PriceCalculator.calculateISS(total);

        // 779.80 * 0.05 = 38.99
        assertEquals(new BigDecimal("38.99"), iss);
    }

    @Test
    void testFullPriceCalculationExample() {
        // Exemplo: 201 DELUXE de 10/03/2027 a 12/03/2027, plano STANDARD
        Room room = new Room("201", RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        BigDecimal subtotal = PriceCalculator.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = PriceCalculator.calculateDiscount(subtotal, RatePlan.STANDARD);
        BigDecimal total = PriceCalculator.calculateTotal(subtotal, discount);

        // 2 noites x 389.90 = 779.80
        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(new BigDecimal("0.00"), discount);
        assertEquals(new BigDecimal("779.80"), total);
    }
}
