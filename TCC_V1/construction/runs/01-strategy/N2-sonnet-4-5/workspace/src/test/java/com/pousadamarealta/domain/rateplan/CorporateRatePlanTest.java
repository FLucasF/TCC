package com.pousadamarealta.domain.rateplan;

import com.pousadamarealta.domain.Room;
import com.pousadamarealta.domain.RoomType;
import com.pousadamarealta.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class CorporateRatePlanTest {

    @Test
    void shouldApplyTecnosulDiscount() {
        CorporateRatePlan ratePlan = new CorporateRatePlan();
        Room room = new Room("201", RoomType.DELUXE);
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        ratePlan.validate(checkIn, checkOut, "TECNOSUL");
        BigDecimal subtotal = ratePlan.calculateSubtotal(room, checkIn, checkOut);
        BigDecimal discount = ratePlan.calculateDiscount(subtotal);

        assertEquals(new BigDecimal("779.80"), subtotal);
        assertEquals(new BigDecimal("93.58"), discount);
    }

    @Test
    void shouldRejectInvalidCompanyCode() {
        CorporateRatePlan ratePlan = new CorporateRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(InvalidRequestException.class, () -> {
            ratePlan.validate(checkIn, checkOut, "INVALID");
        });
    }

    @Test
    void shouldRejectMissingCompanyCode() {
        CorporateRatePlan ratePlan = new CorporateRatePlan();
        LocalDate checkIn = LocalDate.of(2027, 3, 10);
        LocalDate checkOut = LocalDate.of(2027, 3, 12);

        assertThrows(InvalidRequestException.class, () -> {
            ratePlan.validate(checkIn, checkOut, null);
        });
    }
}
