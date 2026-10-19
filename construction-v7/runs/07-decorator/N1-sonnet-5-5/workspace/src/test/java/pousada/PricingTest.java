package pousada;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PricingTest {
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
    void exampleFromSpec() {
        assertEquals(new BigDecimal("599.76"),
                Pricing.subtotal(RoomType.STANDARD, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12)));
    }

    @Test
    void nonRefundableDiscountUsesHalfEven() {
        assertEquals(new BigDecimal("116.97"), RatePlan.NON_REFUNDABLE.discountOn(new BigDecimal("779.80")));
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
    }
}
