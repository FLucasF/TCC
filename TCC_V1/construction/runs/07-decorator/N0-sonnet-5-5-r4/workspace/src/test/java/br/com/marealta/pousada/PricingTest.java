package br.com.marealta.pousada;

import br.com.marealta.pousada.Domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingTest {
    static final Room STD = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));

    @Test
    void highSeasonExample() {
        var q = Pricing.quote(STD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.subtotal());
        assertEquals(new BigDecimal("0.00"), q.discount());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Pricing.highSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.highSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.highSeason(LocalDate.of(2028, 2, 29)));
        assertFalse(Pricing.highSeason(LocalDate.of(2028, 3, 1)));
        assertFalse(Pricing.highSeason(LocalDate.of(2027, 6, 30)));
        assertTrue(Pricing.highSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.highSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void nonRefundableDiscountUsesHalfEven() {
        // 2 noites a 249,90 = 499,80; 15% = 74,97
        var q = Pricing.quote(STD, LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 3), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("499.80"), q.subtotal());
        assertEquals(new BigDecimal("74.97"), q.discount());
        assertEquals(new BigDecimal("424.83"), q.total());
        // meio para o par: 0.125 -> 0.12 ; 0.135 -> 0.14
        assertEquals(new BigDecimal("0.12"), Pricing.money(new BigDecimal("0.125")));
        assertEquals(new BigDecimal("0.14"), Pricing.money(new BigDecimal("0.135")));
    }

    @Test
    void formatsBrl() {
        assertEquals("R$ 1.234,56", Pricing.brl(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Pricing.brl(new BigDecimal("0.00")));
    }
}
