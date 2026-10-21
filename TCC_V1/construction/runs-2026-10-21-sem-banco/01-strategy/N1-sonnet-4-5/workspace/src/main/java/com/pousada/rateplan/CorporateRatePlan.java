package com.pousada.rateplan;

import com.pousada.exception.InvalidRequestException;
import com.pousada.model.Room;
import com.pousada.model.RatePlanType;
import com.pousada.util.DateUtil;
import com.pousada.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class CorporateRatePlan implements RatePlan {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    private String companyCode;

    @Override
    public RatePlanType getType() {
        return RatePlanType.CORPORATE;
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut, int guests, String companyCode) {
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new InvalidRequestException("Código de empresa inválido");
        }
        this.companyCode = companyCode;
    }

    @Override
    public BigDecimal calculateSubtotal(Room room, LocalDate checkIn, LocalDate checkOut) {
        BigDecimal subtotal = BigDecimal.ZERO;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            BigDecimal nightRate = room.getNightlyRate();

            if (DateUtil.isHighSeason(current)) {
                nightRate = MoneyUtil.round(nightRate.multiply(new BigDecimal("1.20")));
            }

            subtotal = subtotal.add(nightRate);
            current = current.plusDays(1);
        }

        return MoneyUtil.round(subtotal);
    }

    @Override
    public BigDecimal calculateDiscount(BigDecimal subtotal) {
        BigDecimal discountRate = COMPANY_DISCOUNTS.get(companyCode);
        return MoneyUtil.round(subtotal.multiply(discountRate));
    }

    @Override
    public BigDecimal calculateRefund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
