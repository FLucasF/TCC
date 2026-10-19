package com.pousada.models;

import java.math.BigDecimal;
import java.time.LocalDate;

public class NonRefundableRatePlan extends RatePlan {
    public NonRefundableRatePlan() {
        super("NON_REFUNDABLE");
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
        return PricingUtils.roundHalfEven(subtotal.multiply(BigDecimal.valueOf(0.15)));
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return BigDecimal.ZERO;
    }
}
