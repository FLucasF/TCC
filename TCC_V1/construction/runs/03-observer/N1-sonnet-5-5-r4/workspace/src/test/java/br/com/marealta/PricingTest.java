package br.com.marealta;

import br.com.marealta.domain.*;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingTest {
    private final RoomCatalog rooms = new RoomCatalog();

    private Quote quote(int room, String in, String out, RatePlan plan) {
        return Quote.of(rooms.require(room), LocalDate.parse(in), LocalDate.parse(out), plan);
    }

    @Test
    void highSeasonExample() {
        Quote q = quote(101, "2027-07-10", "2027-07-12", RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(Money.of("599.76"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.parse("2027-12-14")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-12-15")));
        assertTrue(Season.isHigh(LocalDate.parse("2028-02-29")));
        assertFalse(Season.isHigh(LocalDate.parse("2028-03-01")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-06-30")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-07-31")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-08-01")));
    }

    @Test
    void stayCrossingSeasonPricesEachNight() {
        // noite de 14/12 normal (389.90) + noite de 15/12 alta (467.88)
        Quote q = quote(201, "2027-12-14", "2027-12-16", RatePlan.STANDARD);
        assertEquals(Money.of("857.78"), q.subtotal());
    }

    @Test
    void nonRefundableDiscountRoundsHalfEven() {
        Quote q = quote(101, "2027-03-01", "2027-03-02", RatePlan.NON_REFUNDABLE);
        assertEquals(Money.of("249.90"), q.subtotal());
        assertEquals(Money.of("37.48"), q.discount()); // 37.485 -> par (8)
        assertEquals(Money.of("212.42"), q.total());
    }
}
