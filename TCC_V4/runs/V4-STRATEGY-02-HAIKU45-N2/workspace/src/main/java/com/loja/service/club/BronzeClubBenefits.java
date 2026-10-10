package com.loja.service.club;

import java.math.BigDecimal;

public class BronzeClubBenefits implements ClubBenefits {
    @Override
    public BigDecimal getCreditRate() {
        return BigDecimal.ZERO;
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
