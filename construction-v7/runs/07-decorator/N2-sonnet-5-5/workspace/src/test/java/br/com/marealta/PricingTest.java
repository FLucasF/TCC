package br.com.marealta;

import br.com.marealta.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingTest {
    @Test
    void highSeasonStandard() {
        Quote q = Quote.of(new Room(101, RoomType.STANDARD), LocalDate.parse("2027-07-10"),
                LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void nonRefundableDiscount() {
        Quote q = Quote.of(new Room(201, RoomType.DELUXE), LocalDate.parse("2027-03-10"),
                LocalDate.parse("2027-03-12"), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("779.80"), q.subtotal());
        assertEquals(new BigDecimal("116.97"), q.discount());
        assertEquals(new BigDecimal("662.83"), q.total());
    }

    @Test
    void seasonBoundaries() {
        assertFalse(Season.isHigh(LocalDate.parse("2027-12-14")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-12-15")));
        assertTrue(Season.isHigh(LocalDate.parse("2027-02-28")));
        assertFalse(Season.isHigh(LocalDate.parse("2027-03-01")));
    }
}
