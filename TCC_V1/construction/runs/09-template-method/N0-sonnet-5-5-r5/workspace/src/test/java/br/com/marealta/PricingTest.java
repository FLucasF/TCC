package br.com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.RatePlan;
import br.com.marealta.domain.Room;
import br.com.marealta.domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {
    private final Room standard = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));

    @Test
    void highSeasonBoundaries() {
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2028, 2, 29)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 3, 1)));
        assertTrue(Pricing.isHighSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.isHighSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void highSeasonExample() {
        Quote q = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void nightsAreSeasonedIndividually() {
        // noites de 14/12 (normal) e 15/12 (alta)
        Quote q = Pricing.quote(standard, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), q.subtotal());
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        Quote q = Pricing.quote(standard, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 11), RatePlan.NON_REFUNDABLE);
        // 249.90 * 0.15 = 37.485 -> 37.48 (meio para o par)
        assertEquals(new BigDecimal("37.48"), q.discount());
        assertEquals(new BigDecimal("212.42"), q.total());
    }

    @Test
    void brlFormat() {
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.brl(Money.ZERO));
    }
}
