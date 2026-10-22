package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class StandardRatePlanTest {

    @Test
    void shouldNotApplyDiscount() {
        StandardRatePlan ratePlan = new StandardRatePlan();
        BigDecimal subtotal = new BigDecimal("499.80");

        BigDecimal discount = ratePlan.calculateDiscount(subtotal);

        assertEquals(BigDecimal.ZERO, discount);
    }

    @Test
    void shouldRefundFullAmount() {
        StandardRatePlan ratePlan = new StandardRatePlan();
        BigDecimal amountPaid = new BigDecimal("499.80");

        BigDecimal refund = ratePlan.calculateRefund(amountPaid);

        assertEquals(amountPaid, refund);
    }
}
