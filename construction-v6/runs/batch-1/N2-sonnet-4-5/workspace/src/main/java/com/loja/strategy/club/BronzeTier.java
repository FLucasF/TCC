package com.loja.strategy.club;

import com.loja.dto.CheckoutRequest;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class BronzeTier implements ClubTierStrategy {
    @Override
    public String getCode() {
        return "BRONZE";
    }

    @Override
    public ClubBenefits getBenefits(CheckoutRequest request, BigDecimal productsSubtotal) {
        return new ClubBenefits(BigDecimal.ZERO, false, null);
    }
}
