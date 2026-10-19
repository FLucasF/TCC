package com.pousada.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class LongStayRatePlan extends RatePlan {
    private static final int MIN_NIGHTS = 7;

    public LongStayRatePlan() {
        super("LONG_STAY");
    }

    @Override
    public void validate(int roomCapacity, int guests, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        int nights = PricingUtils.countNights(checkIn, checkOut);
        if (nights < MIN_NIGHTS) {
            throw new ValidationException("INVALID_REQUEST",
                "Estadia longa requer no mínimo 7 noites");
        }
        if (guests < 1 || guests > roomCapacity) {
            throw new ValidationException("CAPACITY_EXCEEDED",
                "Número de hóspedes inválido para a capacidade do quarto");
        }
    }

    @Override
    public BigDecimal calculateSubtotal(BigDecimal nightlyRate, int nights, LocalDate checkIn, LocalDate checkOut, Room.RoomType roomType) {
        BigDecimal total = BigDecimal.ZERO;
        LocalDate current = checkIn;
        while (current.isBefore(checkOut)) {
            BigDecimal rate = PricingUtils.getNightlyRateWithSeason(nightlyRate, current);
            total = total.add(rate);
            current = current.plusDays(1);
        }
        return total;
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        return PricingUtils.roundHalfEven(subtotal.multiply(BigDecimal.valueOf(0.10)));
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getStatus() == Reservation.ReservationStatus.PENDING) {
            return BigDecimal.ZERO;
        }
        return reservation.getAmountPaid();
    }
}
