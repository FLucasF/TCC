package com.loja.service.club;

import java.math.BigDecimal;

public class OuroClubBenefits implements ClubBenefits {
    private static final BigDecimal MIN_SUBTOTAL_FOR_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal getCreditRate() {
        return new BigDecimal("0.05");
    }

    @Override
    public boolean shouldWaiveFrete() {
        return true;
    }

    @Override
    public boolean shouldAddBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MIN_SUBTOTAL_FOR_BRINDE) > 0;
    }
}
