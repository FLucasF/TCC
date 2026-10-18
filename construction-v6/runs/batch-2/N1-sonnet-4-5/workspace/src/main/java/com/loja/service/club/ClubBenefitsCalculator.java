package com.loja.service.club;

import com.loja.domain.ClubTier;
import com.loja.domain.ShippingMethod;
import com.loja.util.MoneyUtils;

import java.math.BigDecimal;

public class ClubBenefitsCalculator {
    public BigDecimal calculateCredit(BigDecimal productsSubtotal, ClubTier clubTier) {
        return switch (clubTier) {
            case BRONZE -> MoneyUtils.roundMoney(BigDecimal.ZERO);
            case SILVER -> MoneyUtils.roundMoney(productsSubtotal.multiply(new BigDecimal("0.02")));
            case GOLD -> MoneyUtils.roundMoney(productsSubtotal.multiply(new BigDecimal("0.05")));
        };
    }

    public boolean hasFreeShipping(ClubTier clubTier, ShippingMethod shippingMethod) {
        return clubTier == ClubTier.GOLD && shippingMethod != ShippingMethod.COURIER;
    }

    public boolean hasGift(ClubTier clubTier, BigDecimal productsSubtotal) {
        return clubTier == ClubTier.GOLD && productsSubtotal.compareTo(new BigDecimal("500.00")) > 0;
    }
}
