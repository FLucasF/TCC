package com.loja.checkout.dominio;

import java.math.BigDecimal;

public interface CalculadoraFrete {
    BigDecimal calcular(BigDecimal pesoTotal);
    int prazoEmDias();
    boolean aceitaPedido(BigDecimal pesoTotal);
}
