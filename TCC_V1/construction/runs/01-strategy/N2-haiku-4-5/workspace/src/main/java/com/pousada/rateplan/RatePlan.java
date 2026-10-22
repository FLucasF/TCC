package com.pousada.rateplan;

import com.pousada.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;

public interface RatePlan {
    void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) throws RatePlanException;

    PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut);

    BigDecimal calculateRefund(BigDecimal amountPaid);

    record PriceCalculation(BigDecimal subtotal, BigDecimal discount) {}

    class RatePlanException extends Exception {
        private final String code;

        public RatePlanException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
