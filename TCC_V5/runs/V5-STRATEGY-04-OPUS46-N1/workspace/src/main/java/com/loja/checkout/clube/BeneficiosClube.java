package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface BeneficiosClube {

    String getCodigo();

    boolean isFreteGratis();

    BigDecimal calcularCredito(BigDecimal subtotalProdutos);

    boolean ganhaBrinde(BigDecimal subtotalProdutos);
}
