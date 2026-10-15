package com.loja.checkout.domain.cupom;

import java.math.BigDecimal;

public interface Cupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete);
    boolean verificarAplicavel(BigDecimal subtotalProdutos);
}
