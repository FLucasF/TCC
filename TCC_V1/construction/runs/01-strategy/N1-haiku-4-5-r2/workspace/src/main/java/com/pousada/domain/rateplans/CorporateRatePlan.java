package com.pousada.domain.rateplans;

import com.pousada.domain.Money;
import com.pousada.domain.Room;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan extends BaseRatePlan {
    private static final Map<String, Double> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", 12.0,
        "AGROVALE", 18.0,
        "FARMAVIDA", 25.0
    );

    @Override
    public String getId() {
        return "CORPORATE";
    }

    @Override
    public void validate(int roomNumber, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        validateBasic(guests);
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new InvalidReservationException("INVALID_REQUEST", "Código de empresa inválido");
        }
    }

    @Override
    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut) {
        throw new IllegalStateException("calculatePrice deve ser chamado com companyCode");
    }

    public PriceCalculation calculatePrice(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode) {
        BigDecimal subtotal = calculateSubtotal(room, checkIn, checkOut);
        Double discountPercent = COMPANY_DISCOUNTS.get(companyCode);
        BigDecimal discount = Money.percentOf(subtotal, discountPercent);
        BigDecimal total = Money.round(subtotal.subtract(discount));
        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
