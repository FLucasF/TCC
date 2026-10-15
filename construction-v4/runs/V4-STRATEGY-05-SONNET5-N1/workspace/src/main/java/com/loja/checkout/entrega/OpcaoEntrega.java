package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface OpcaoEntrega {

    String codigo();

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoEntregaDias();

    boolean disponivel(BigDecimal pesoTotalKg);
}
