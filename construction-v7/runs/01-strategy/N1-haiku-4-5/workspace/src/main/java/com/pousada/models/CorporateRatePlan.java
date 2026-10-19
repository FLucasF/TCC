package com.pousada.models;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan extends RatePlan {
    private static final Map<String, BigDecimal> CORPORATE_DISCOUNTS = Map.of(
        "TECNOSUL", BigDecimal.valueOf(0.12),
        "AGROVALE", BigDecimal.valueOf(0.18),
        "FARMAVIDA", BigDecimal.valueOf(0.25)
    );

    private String companyCode;
    private BigDecimal discountRate;

    public CorporateRatePlan() {
        super("CORPORATE");
    }

    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
        this.discountRate = CORPORATE_DISCOUNTS.get(companyCode);
    }

    public String getCompanyCode() {
        return companyCode;
    }

    @Override
    public void validate(int roomCapacity, int guests, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        if (companyCode == null || !CORPORATE_DISCOUNTS.containsKey(companyCode)) {
            throw new ValidationException("INVALID_REQUEST",
                "Código de empresa inválido");
        }
        if (guests < 1 || guests > roomCapacity) {
            throw new ValidationException("CAPACITY_EXCEEDED",
                "Número de hóspedes inválido para a capacidade do quarto");
        }
        setCompanyCode(companyCode);
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
        return PricingUtils.roundHalfEven(subtotal.multiply(discountRate));
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        if (reservation.getStatus() == Reservation.ReservationStatus.PENDING) {
            return BigDecimal.ZERO;
        }
        return reservation.getAmountPaid();
    }
}
