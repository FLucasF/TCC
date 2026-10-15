package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface BeneficioClube {

    BigDecimal credito(BigDecimal subtotalProdutos);

    BigDecimal frete(BigDecimal freteCalculado);

    boolean brinde(BigDecimal subtotalProdutos);
}
