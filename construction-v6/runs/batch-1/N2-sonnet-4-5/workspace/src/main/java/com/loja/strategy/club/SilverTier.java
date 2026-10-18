package com.loja.strategy.club;

import com.loja.dto.CheckoutRequest;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class SilverTier implements ClubTierStrategy {
    @Override
    public String getCode() {
        return "SILVER";
    }

    @Override
    public ClubBenefits getBenefits(CheckoutRequest request, BigDecimal productsSubtotal) {
        return new ClubBenefits(new BigDecimal("0.02"), false, null);
    }
}
