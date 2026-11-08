package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Room;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {
    private final Room standard = Config.ROOMS.get(0);

    @Test
    void highSeasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.of(2026, 12, 14)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2026, 12, 15)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 2, 28)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 1)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void exampleFromTheOwner() {
        var q = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void nonRefundableDiscountUsesHalfEven() {
        // 2 noites normais: 499.80; 15% = 74.97
        var q = Pricing.quote(standard, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("499.80"), q.subtotal());
        assertEquals(Money.of("74.97"), q.discount());
        assertEquals(Money.of("424.83"), q.total());
        // 249.90 * 0.15 = 37.485 -> meio para o par = 37.48
        var one = Pricing.quote(standard, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("37.48"), one.discount());
        assertEquals(Money.of("212.42"), one.total());
    }

    @Test
    void stayCrossingIntoHighSeasonPricesEachNight() {
        var q = Pricing.quote(standard, LocalDate.of(2026, 12, 14), LocalDate.of(2026, 12, 16), RatePlan.STANDARD);
        assertEquals(Money.of("549.78"), q.subtotal()); // 249.90 + 299.88
    }
}
