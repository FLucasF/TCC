package br.com.marealta;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.PricingCalculator;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingTest {
    private final Room standard = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));
    private final Room deluxe = new Room(201, RoomType.DELUXE, 3, new BigDecimal("389.90"));

    @Test
    void highSeasonBoundaries() {
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2026, 12, 14)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2026, 12, 15)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2028, 2, 29)));
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertTrue(PricingCalculator.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(PricingCalculator.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void highSeasonExample() {
        Quote q = PricingCalculator.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void nonRefundableDiscountUsesHalfEven() {
        // 2 noites normais de 389.90 = 779.80; 15% = 116.97 exato
        Quote q = PricingCalculator.quote(deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
        // 249.90 * 0.15 = 37.485 -> meio para o par = 37.48
        Quote one = PricingCalculator.quote(standard, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("37.48"), one.discount());
        assertEquals(new BigDecimal("212.42"), one.total());
    }

    @Test
    void stayCrossingSeasonBoundary() {
        Quote q = PricingCalculator.quote(standard, LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 16), RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), q.total()); // 249.90 + 299.88
    }

    @Test
    void moneyFormat() {
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.format(Money.ZERO));
        assertEquals("R$ 1.234.567,00", Money.format(new BigDecimal("1234567")));
    }
}
