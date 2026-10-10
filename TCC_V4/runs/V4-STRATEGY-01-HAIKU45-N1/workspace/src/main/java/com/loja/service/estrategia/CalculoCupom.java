package com.loja.service.estrategia;

import java.math.BigDecimal;

public interface CalculoCupom {
    BigDecimal calcularDesconto(BigDecimal subtotalProdutos, BigDecimal frete);
    BigDecimal calcularDescontoFrete(BigDecimal frete);
    boolean aplicavel(BigDecimal subtotalProdutos);
}
