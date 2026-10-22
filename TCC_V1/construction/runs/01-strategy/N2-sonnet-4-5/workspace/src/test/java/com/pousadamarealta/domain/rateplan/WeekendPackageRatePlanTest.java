package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class WeekendPackageRatePlanTest {

    @Test
    void shouldCalculateFixedPackagePrice() {
        WeekendPackageRatePlan ratePlan = new WeekendPackageRatePlan();
        Room room = new Room("201", RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 14);

        ratePlan.validate(checkIn, checkOut, null);
        BigDecimal subtotal = ratePlan.calculateSubtotal(room, checkIn, checkOut);

        assertEquals(new BigDecimal("820.00"), subtotal);
    }

    @Test
    void shouldRefundHalfAmount() {
        WeekendPackageRatePlan ratePlan = new WeekendPackageRatePlan();
        BigDecimal amountPaid = new BigDecimal("820.00");

        BigDecimal refund = ratePlan.calculateRefund(amountPaid);

        assertEquals(new BigDecimal("410.00"), refund);
    }

    @Test
    void shouldRejectNonFridayCheckIn() {
        WeekendPackageRatePlan ratePlan = new WeekendPackageRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 13);
        LocalDate checkOut = LocalDate.of(2027, 3, 15);

        assertThrows(InvalidRequestException.class, () -> {
            ratePlan.validate(checkIn, checkOut, null);
        });
    }

    @Test
    void shouldRejectNonTwoNights() {
        WeekendPackageRatePlan ratePlan = new WeekendPackageRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 12);
        LocalDate checkOut = LocalDate.of(2027, 3, 15);

        assertThrows(InvalidRequestException.class, () -> {
            ratePlan.validate(checkIn, checkOut, null);
        });
    }
}
