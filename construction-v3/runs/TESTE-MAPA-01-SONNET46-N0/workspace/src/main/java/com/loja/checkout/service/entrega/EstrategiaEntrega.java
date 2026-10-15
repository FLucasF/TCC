package com.loja.checkout.service.entrega;

import java.math.BigDecimal;

public interface EstrategiaEntrega {
    String getCodigo();
    boolean atendePedido(BigDecimal pesoTotalKg);
    BigDecimal calcularFrete(BigDecimal pesoTotalKg);
    int prazoEmDias();
}
