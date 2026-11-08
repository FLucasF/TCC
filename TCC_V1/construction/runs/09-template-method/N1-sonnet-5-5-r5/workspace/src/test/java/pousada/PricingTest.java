package pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import pousada.domain.Money;
import pousada.domain.Pricing;
import pousada.domain.RatePlan;
import pousada.domain.Room;
import pousada.domain.RoomType;

class PricingTest {
    private final Room standard = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        var quote = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), quote.total());
        assertEquals(2, quote.nights());
    }

    @Test
    void seasonBoundaries() {
        // 14/12 é baixa, 15/12 é alta; 28/02 é alta, 01/03 é baixa
        assertEquals(new BigDecimal("249.90"), Pricing.quote(standard, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 15), RatePlan.STANDARD).total());
        assertEquals(new BigDecimal("299.88"), Pricing.quote(standard, LocalDate.of(2027, 12, 15), LocalDate.of(2027, 12, 16), RatePlan.STANDARD).total());
        assertEquals(new BigDecimal("299.88"), Pricing.quote(standard, LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 1), RatePlan.STANDARD).total());
        assertEquals(new BigDecimal("249.90"), Pricing.quote(standard, LocalDate.of(2028, 3, 1), LocalDate.of(2028, 3, 2), RatePlan.STANDARD).total());
    }

    @Test
    void nonRefundableDiscountsTheSubtotal() {
        var quote = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("599.76"), quote.subtotal());
        assertEquals(new BigDecimal("89.96"), quote.discount()); // 89.964
        assertEquals(new BigDecimal("509.80"), quote.total());
    }

    @Test
    void roundsHalfToEven() {
        assertEquals(new BigDecimal("0.12"), Money.round(new BigDecimal("0.125")));
        assertEquals(new BigDecimal("0.14"), Money.round(new BigDecimal("0.135")));
    }

    @Test
    void formatsBrazilianMoney() {
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
        assertEquals("1234.56", Money.csv(new BigDecimal("1234.56")));
    }
}
