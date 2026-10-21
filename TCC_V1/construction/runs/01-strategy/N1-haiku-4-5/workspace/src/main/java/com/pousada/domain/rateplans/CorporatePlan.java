package com.pousada.domain.rateplans;

import com.pousada.domain.Room;
import com.pousada.util.DateUtils;
import com.pousada.util.Money;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class CorporatePlan extends RatePlan {
    private static final Map<String, Double> COMPANY_DISCOUNTS = new HashMap<>();

    static {
        COMPANY_DISCOUNTS.put("TECNOSUL", 0.12);
        COMPANY_DISCOUNTS.put("AGROVALE", 0.18);
        COMPANY_DISCOUNTS.put("FARMAVIDA", 0.25);
    }

    private final String companyCode;

    public CorporatePlan(String companyCode) {
        this.companyCode = companyCode;
    }

    @Override
    public String getPlanName() {
        return "CORPORATE";
    }

    @Override
    public void validate(Room room, LocalDate checkIn, LocalDate checkOut,
                        int guests, String companyCode) {
        if (companyCode == null || !COMPANY_DISCOUNTS.containsKey(companyCode)) {
            throw new IllegalArgumentException("Invalid company code");
        }
    }

    @Override
    public PriceCalculation calculatePrices(Room room, LocalDate checkIn, LocalDate checkOut) {
        double discountRate = COMPANY_DISCOUNTS.get(companyCode);

        double subtotal = 0.0;
        LocalDate current = checkIn;

        while (current.isBefore(checkOut)) {
            double nightlyRate = room.getNightlyRate();
            if (DateUtils.isHighSeason(current)) {
                nightlyRate *= 1.20;
            }
            subtotal += Money.round(nightlyRate);
            current = current.plusDays(1);
        }

        subtotal = Money.round(subtotal);
        double discount = Money.round(subtotal * discountRate);
        double total = Money.round(subtotal - discount);

        return new PriceCalculation(subtotal, discount, total);
    }

    @Override
    public double calculateRefund(double amountPaid) {
        return amountPaid;
    }
}
