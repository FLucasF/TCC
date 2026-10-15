package com.loja.strategy.cupom;

import java.math.BigDecimal;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete);
    boolean aplicavel(BigDecimal subtotalProdutos);
}
