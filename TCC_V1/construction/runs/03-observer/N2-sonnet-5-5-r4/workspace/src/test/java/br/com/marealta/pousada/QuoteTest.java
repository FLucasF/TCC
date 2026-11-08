package br.com.marealta.pousada;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

import br.com.marealta.pousada.domain.*;

import static org.junit.jupiter.api.Assertions.*;

class QuoteTest {
    private final RoomCatalog rooms = new RoomCatalog();

    @Test
    void highSeasonStandard() {
        Quote q = Quote.of(rooms.find(101).get(), LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new java.math.BigDecimal("599.76"), q.total());
        assertEquals(new java.math.BigDecimal("0.00"), q.discount());
    }

    @Test
    void nonRefundableDiscount() {
        Quote q = Quote.of(rooms.find(201).get(), LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-12"), RatePlan.NON_REFUNDABLE);
        assertEquals(new java.math.BigDecimal("779.80"), q.subtotal());
        assertEquals(new java.math.BigDecimal("116.97"), q.discount());
        assertEquals(new java.math.BigDecimal("662.83"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Quote.isHighSeason(LocalDate.parse("2027-12-14")));
        assertTrue(Quote.isHighSeason(LocalDate.parse("2027-12-15")));
        assertTrue(Quote.isHighSeason(LocalDate.parse("2028-02-29")));
        assertFalse(Quote.isHighSeason(LocalDate.parse("2027-03-01")));
        assertTrue(Quote.isHighSeason(LocalDate.parse("2027-07-31")));
    }

    @Test
    void moneyFormat() {
        assertEquals("R$ 1.234,56", Money.format(new java.math.BigDecimal("1234.56")));
    }
}
