package br.com.marealta;

import br.com.marealta.domain.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PricingTest {
    private final Room standard = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));

    @Test
    void highSeasonExample() {
        Quote q = Quote.of(standard, LocalDate.parse("2027-07-10"), LocalDate.parse("2027-07-12"), RatePlan.STANDARD);
        assertEquals(2, q.nights());
        assertEquals(new BigDecimal("599.76"), q.total());
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
    void nonRefundableDiscountIsRoundedHalfEven() {
        Quote q = Quote.of(standard, LocalDate.parse("2027-03-10"), LocalDate.parse("2027-03-11"), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("249.90"), q.subtotal());
        assertEquals(new BigDecimal("37.48"), q.discount()); // 37.485 -> 37.48
        assertEquals(new BigDecimal("212.42"), q.total());
    }

    @Test
    void moneyFormat() {
        assertEquals("R$ 1.234,56", Money.brl(new BigDecimal("1234.56")));
        assertEquals("R$ 0,00", Money.brl(Money.ZERO));
    }
}
