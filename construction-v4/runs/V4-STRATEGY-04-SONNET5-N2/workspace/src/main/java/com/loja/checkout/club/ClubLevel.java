package com.loja.checkout.club;

import java.math.BigDecimal;

public interface ClubLevel {

    String getCodigo();

    boolean isFreteGratis();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean temBrinde(BigDecimal subtotalProdutos);
}
