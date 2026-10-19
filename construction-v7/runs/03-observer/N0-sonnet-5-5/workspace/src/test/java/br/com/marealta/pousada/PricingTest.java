package br.com.marealta.pousada;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.marealta.pousada.domain.Money;
import br.com.marealta.pousada.domain.Pricing;
import br.com.marealta.pousada.domain.RatePlan;
import br.com.marealta.pousada.domain.Room;
import br.com.marealta.pousada.domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {
    @Test
    void highSeasonStandard() {
        var q = Pricing.quote(new Room(101, RoomType.STANDARD), LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12),
                RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void seasonBoundaries() {
        assertEquals(false, Pricing.highSeason(LocalDate.of(2027, 12, 14)));
        assertEquals(true, Pricing.highSeason(LocalDate.of(2027, 12, 15)));
        assertEquals(true, Pricing.highSeason(LocalDate.of(2028, 2, 29)));
        assertEquals(false, Pricing.highSeason(LocalDate.of(2027, 3, 1)));
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        var q = Pricing.quote(new Room(201, RoomType.DELUXE), LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12),
                RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    @Test
    void formatsMoney() {
        assertEquals("R$ 1.234,56", Money.format(new BigDecimal("1234.56")));
    }
}
