package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class LongStayRatePlanTest {

    @Test
    void shouldApply10PercentDiscount() {
        LongStayRatePlan ratePlan = new LongStayRatePlan();
        Room room = new Room("101", RoomType.STANDARD);
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 8);

        ratePlan.validate(checkIn, checkOut, null);
        BigDecimal subtotal = ratePlan.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = ratePlan.calculateDiscount(subtotal);

        assertEquals(new BigDecimal("1749.30"), subtotal);
        assertEquals(new BigDecimal("174.93"), discount);
    }

    @Test
    void shouldRejectLessThan7Nights() {
        LongStayRatePlan ratePlan = new LongStayRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 1);
        LocalDate checkOut = LocalDate.of(2027, 3, 7);

        assertThrows(InvalidRequestException.class, () -> {
            ratePlan.validate(checkIn, checkOut, null);
        });
    }
}
