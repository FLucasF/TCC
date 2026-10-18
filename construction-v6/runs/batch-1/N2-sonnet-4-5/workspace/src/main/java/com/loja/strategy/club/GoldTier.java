package com.loja.strategy.club;

import com.loja.dto.CheckoutRequest;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class GoldTier implements ClubTierStrategy {
    @Override
    public String getCode() {
        return "GOLD";
    }

    @Override
    public ClubBenefits getBenefits(CheckoutRequest request, BigDecimal productsSubtotal) {
        boolean freeShipping = !request.getShippingMethod().equals("COURIER");
        BigDecimal giftThreshold = new BigDecimal("500.00");

        return new ClubBenefits(new BigDecimal("0.05"), freeShipping, giftThreshold);
    }
}
