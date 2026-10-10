package com.loja.checkout.dominio;

import java.math.BigDecimal;

public interface BeneficiosClube {
    BigDecimal calcularCredito(BigDecimal subtotalProdutos);
    boolean temFreteGratis();
    boolean temBrinde(BigDecimal subtotalProdutos);
}
