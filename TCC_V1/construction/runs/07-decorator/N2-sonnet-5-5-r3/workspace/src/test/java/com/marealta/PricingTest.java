package com.marealta;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.marealta.domain.Pricing;
import com.marealta.domain.RatePlan;
import com.marealta.domain.Room;
import com.marealta.domain.RoomType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PricingTest {
    private final Room standard = new Room(101, RoomType.STANDARD, 2, new BigDecimal("249.90"));

    @Test
    void highSeasonNights() {
        var q = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.STANDARD);
        assertEquals(new BigDecimal("599.76"), q.total());
        assertEquals(2, q.nights());
    }

    @Test
    void seasonBoundaries() {
        var q = Pricing.quote(standard, LocalDate.of(2027, 12, 14), LocalDate.of(2027, 12, 16), RatePlan.STANDARD);
        assertEquals(new BigDecimal("549.78"), q.subtotal());
    }

    @Test
    void nonRefundableDiscount() {
        var q = Pricing.quote(standard, LocalDate.of(2027, 7, 10), LocalDate.of(2027, 7, 12), RatePlan.NON_REFUNDABLE);
        assertEquals(new BigDecimal("89.96"), q.discount());
        assertEquals(new BigDecimal("509.80"), q.total());
    }
}
