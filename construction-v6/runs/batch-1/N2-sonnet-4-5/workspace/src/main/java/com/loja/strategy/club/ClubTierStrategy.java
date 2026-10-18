package com.loja.strategy.club;

import com.loja.dto.CheckoutRequest;
import java.math.BigDecimal;

public interface ClubTierStrategy {
    String getCode();
    ClubBenefits getBenefits(CheckoutRequest request, BigDecimal productsSubtotal);
}
