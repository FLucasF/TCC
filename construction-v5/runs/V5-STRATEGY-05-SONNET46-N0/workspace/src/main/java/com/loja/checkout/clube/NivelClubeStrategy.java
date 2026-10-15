package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClubeStrategy {
    String getCodigo();
    boolean isFreteGratis();
    BigDecimal getPercentualCredito();
    boolean temBrinde(BigDecimal subtotalProdutos);
}
