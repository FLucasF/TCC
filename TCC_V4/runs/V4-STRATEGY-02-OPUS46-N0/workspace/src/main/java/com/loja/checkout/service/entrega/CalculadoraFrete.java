package com.loja.checkout.service.entrega;

import java.math.BigDecimal;

public interface CalculadoraFrete {

    String codigo();

    boolean disponivel(BigDecimal pesoTotal);

    BigDecimal calcularFrete(BigDecimal pesoTotal);

    int prazoEntregaDias();
}
