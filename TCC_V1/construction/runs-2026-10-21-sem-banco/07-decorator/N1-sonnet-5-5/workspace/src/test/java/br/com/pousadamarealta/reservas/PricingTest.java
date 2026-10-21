package br.com.pousadamarealta.reservas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.pousadamarealta.reservas.domain.Money;
import br.com.pousadamarealta.reservas.domain.Pricing;
import br.com.pousadamarealta.reservas.domain.Quote;
import br.com.pousadamarealta.reservas.domain.RatePlan;
import br.com.pousadamarealta.reservas.domain.Room;
import br.com.pousadamarealta.reservas.domain.RoomType;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {

    @Test
    void highSeasonNightsCostTwentyPercentMore() {
        Quote q = Pricing.quote(Room.of(101, RoomType.STANDARD), LocalDate.parse("2027-07-10"),
                LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void nonRefundableDiscountsTheSubtotal() {
        Quote q = Pricing.quote(Room.of(201, RoomType.DELUXE), LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("779.80"), q.subtotal());
        assertEquals(Money.of("116.97"), q.discount());
        assertEquals(Money.of("662.83"), q.total());
    }

    @Test
    void seasonBoundaries() {
        Room room = Room.of(301, RoomType.SUITE);
        Quote dec14 = Pricing.quote(room, LocalDate.parse("2026-12-14"), LocalDate.parse("2026-12-15"), RatePlan.STANDARD);
        Quote dec15 = Pricing.quote(room, LocalDate.parse("2026-12-15"), LocalDate.parse("2026-12-16"), RatePlan.STANDARD);
        Quote mar1 = Pricing.quote(room, LocalDate.parse("2027-03-01"), LocalDate.parse("2027-03-02"), RatePlan.STANDARD);
        assertEquals(Money.of("579.90"), dec14.total());
        assertEquals(Money.of("695.88"), dec15.total());
        assertEquals(Money.of("579.90"), mar1.total());
    }
}
