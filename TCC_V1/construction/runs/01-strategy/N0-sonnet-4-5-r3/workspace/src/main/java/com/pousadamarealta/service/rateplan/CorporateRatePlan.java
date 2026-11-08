package com.pousadamarealta.service.rateplan;

import com.pousadamarealta.domain.Reservation;
import com.pousadamarealta.domain.Room;
import com.pousadamarealta.exception.ApiException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class CorporateRatePlan extends StandardRatePlan {

    private static final Map<String, BigDecimal> COMPANY_DISCOUNTS = Map.of(
        "TECNOSUL", new BigDecimal("0.12"),
        "AGROVALE", new BigDecimal("0.18"),
        "FARMAVIDA", new BigDecimal("0.25")
    );

    @Override
    public void validate(Reservation reservation, Room room) {
        String companyCode = reservation.getCompanyCode();
        if (companyCode == null || companyCode.isEmpty()) {
            throw new ApiException(400, "INVALID_REQUEST", "Company code is required for corporate rate plan");
        }
        if (!COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new ApiException(400, "INVALID_REQUEST", "Invalid company code: " + companyCode);
        }
    }

    @Override
    public void calculatePricing(Reservation reservation, Room room) {
        super.calculatePricing(reservation, room);

        BigDecimal discountRate = COMPANY_DISCOUNTS.get(reservation.getCompanyCode());
        BigDecimal discount = reservation.getSubtotal()
            .multiply(discountRate)
            .setScale(2, RoundingMode.HALF_EVEN);

        BigDecimal total = reservation.getSubtotal()
            .subtract(discount)
            .setScale(2, RoundingMode.HALF_EVEN);

        reservation.setDiscount(discount);
        reservation.setTotal(total);
    }

    @Override
    public BigDecimal calculateRefund(Reservation reservation) {
        return reservation.getAmountPaid();
    }
}
