package com.loja.service.club;

import java.math.BigDecimal;

public class PrataClubBenefits implements ClubBenefits {
    @Override
    public BigDecimal getCreditRate() {
        return new BigDecimal("0.02");
    }

    @Override
    public boolean shouldWaiveFrete() {
        return false;
    }

    @Override
    public boolean shouldAddBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
