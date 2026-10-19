package com.pousada.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class StandardRatePlan extends RatePlan {
    public StandardRatePlan() {
        super("STANDARD");
    }

    @Override
    public void validate(int roomCapacity, int guests, LocalDate checkIn, LocalDate checkOut, String companyCode) {
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
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getStatus() == Reservation.ReservationStatus.PENDING) {
            return BigDecimal.ZERO;
        }
        return reservation.getAmountPaid();
    }
}
