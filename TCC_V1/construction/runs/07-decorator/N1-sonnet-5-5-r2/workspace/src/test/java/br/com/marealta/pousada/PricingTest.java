package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.marealta.pousada.domain.Pricing;
import br.com.marealta.pousada.domain.Quote;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {
    private final Room standard = new Room(101, "STANDARD", 2, new BigDecimal("249.90"));
    private final Room deluxe = new Room(201, "DELUXE", 3, new BigDecimal("389.90"));

    @Test
    void highSeasonNights() {
        Quote q = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Pricing.highSeason(LocalDate.of(2027, 12, 14)));
        assertTrue(Pricing.highSeason(LocalDate.of(2027, 12, 15)));
        assertTrue(Pricing.highSeason(LocalDate.of(2028, 2, 29)));
        assertFalse(Pricing.highSeason(LocalDate.of(2027, 3, 1)));
        assertTrue(Pricing.highSeason(LocalDate.of(2027, 7, 31)));
        assertFalse(Pricing.highSeason(LocalDate.of(2027, 8, 1)));
    }

    @Test
    void nonRefundableDiscount() {
        Quote q = Pricing.quote(deluxe, LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    @Test
    void halfEvenRounding() {
        Room room = new Room(1, "X", 1, new BigDecimal("0.25"));
        // 0.25 * 1.20 = 0.30; subtotal 0.30 * 0.15 = 0.045 -> 0.04 (meio para o par)
        Quote q = Pricing.quote(room, LocalDate.of(2027, 7, 1), LocalDate.of(2027, 7, 2), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("0.04"), q.discount());
    }
}
