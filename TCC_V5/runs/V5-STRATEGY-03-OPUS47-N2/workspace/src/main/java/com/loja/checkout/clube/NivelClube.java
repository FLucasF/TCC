package com.loja.checkout.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal ajustaFrete(BigDecimal freteBase);
    BigDecimal credito(BigDecimal subtotalProdutos);
    boolean brinde(BigDecimal subtotalProdutos);
}
