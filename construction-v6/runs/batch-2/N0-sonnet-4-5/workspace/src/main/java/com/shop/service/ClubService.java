package com.shop.service;

import com.shop.model.enums.ClubTier;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class ClubService {

    public BigDecimal calculateCredit(ClubTier tier, BigDecimal productsSubtotal) {
        BigDecimal percentage = switch (tier) {
            case BRONZE -> BigDecimal.ZERO;
            case SILVER -> new BigDecimal("0.02");
            case GOLD -> new BigDecimal("0.05");
        };

        return productsSubtotal.multiply(percentage)
                .setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean hasGift(ClubTier tier, BigDecimal productsSubtotal) {
        return tier == ClubTier.GOLD && productsSubtotal.compareTo(new BigDecimal("500.00")) > 0;
    }

    public boolean hasFreeShipping(ClubTier tier) {
        return tier == ClubTier.GOLD;
    }
}
