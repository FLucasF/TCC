package com.loja.checkout.domain;

import java.math.BigDecimal;

public interface Regiao {
    BigDecimal calcularSeguro(BigDecimal subtotalProdutos);
}
