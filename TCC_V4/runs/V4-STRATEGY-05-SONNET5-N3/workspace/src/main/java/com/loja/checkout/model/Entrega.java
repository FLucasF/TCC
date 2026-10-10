package com.loja.checkout.model;

import java.math.BigDecimal;

public interface Entrega {

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int prazoEntregaDias();

    boolean disponivel(BigDecimal pesoTotalKg);
}
