package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Quote;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.RoomType;
import br.com.marealta.pousada.domain.Season;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {

    @Test
    void highSeasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.of(2027, 12, 14)));
        assertTrue(Season.isHigh(LocalDate.of(2027, 12, 15)));
        assertTrue(Season.isHigh(LocalDate.of(2028, 2, 29)));
        assertFalse(Season.isHigh(LocalDate.of(2027, 3, 1)));
        assertTrue(Season.isHigh(LocalDate.of(2027, 7, 1)));
        assertTrue(Season.isHigh(LocalDate.of(2027, 7, 31)));
        assertFalse(Season.isHigh(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void highSeasonStandardTwoNights() {
        Quote quote = Quote.of(RoomType.STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, quote.nights());
        assertEquals(new BigDecimal("599.76"), quote.subtotal());
        assertEquals(new BigDecimal("0.00"), quote.discount());
        assertEquals(new BigDecimal("599.76"), quote.total());
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        Quote quote = Quote.of(RoomType.DELUXE, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), quote.subtotal());
        assertEquals(new BigDecimal("116.97"), quote.discount());
        assertEquals(new BigDecimal("662.83"), quote.total());
    }

    @Test
    void stayCrossingSeasonBoundaryPricesEachNight() {
        Quote quote = Quote.of(RoomType.SUITE, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);
        assertEquals(new BigDecimal("1275.78"), quote.subtotal()); // 579.90 + 695.88
    }

    @Test
    void formatsMoneyInBrazilianStyle() {
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.format(Money.ZERO));
    }
}
