package com.marealta.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class QuoteTest {
    private final RoomCatalog rooms = new RoomCatalog();

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Quote q = Quote.of(rooms.require(101), LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(Money.of("599.76"), q.subtotal());
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        Room room = rooms.require(101);
        // 14/12 normal, 15/12 alta
        Quote q = Quote.of(room, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);
        assertEquals(Money.of("549.78"), q.subtotal());
        // fim de fevereiro (ano bissexto) é alta, 1º de março é normal
        Quote q2 = Quote.of(room, LocalDate.of(2028, 2, 29), LocalDate.of(2028, 3, 2), RatePlan.STANDARD);
        assertEquals(Money.of("549.78"), q2.subtotal());
    }

    @Test
    void nonRefundableDiscountIsRoundedHalfEven() {
        Quote q = Quote.of(rooms.require(201), LocalDate.of(2027, 3, 10), LocalDate.of(2027, 3, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("116.97"), q.discount());
        assertEquals(Money.of("662.83"), q.total());
    }
}
