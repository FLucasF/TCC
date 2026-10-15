package com.loja.service.club;

import java.math.BigDecimal;

public interface ClubBenefits {
    BigDecimal getCreditRate();
    boolean shouldWaiveFrete();
    boolean shouldAddBrinde(BigDecimal subtotalProdutos);
}
