package com.pousada.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public abstract class RatePlan {
    protected String code;

    public RatePlan(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public abstract void validate(int roomCapacity, int guests, LocalDate checkIn, LocalDate checkOut, String companyCode);

    public abstract BigDecimal calculateSubtotal(BigDecimal nightlyRate, int nights, LocalDate checkIn, LocalDate checkOut, Room.RoomType roomType);

    public abstract BigDecimal calculateDiscount(BigDecimal subtotal);

    public abstract BigDecimal calculateRefund(Reservation reservation);

    public static class ValidationException extends RuntimeException {
        private final String code;

        public ValidationException(String code, String message) {
            super(message);
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
