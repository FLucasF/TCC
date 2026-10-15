package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;

public interface NivelClube {
    BigDecimal fretePago(BigDecimal freteBase);

    BigDecimal credito(BigDecimal subtotalProdutos);

    boolean brinde(BigDecimal subtotalProdutos);
}
