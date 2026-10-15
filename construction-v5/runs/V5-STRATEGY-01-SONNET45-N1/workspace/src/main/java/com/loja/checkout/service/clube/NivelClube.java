package com.loja.checkout.service.clube;

import java.math.BigDecimal;

public interface NivelClube {
    String getCodigo();
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean temFreteGratis();
    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
