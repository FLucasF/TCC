package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntregaStrategy {
    String codigo();
    boolean atende(BigDecimal pesoKgTotal);
    BigDecimal calcularFrete(BigDecimal pesoKgTotal);
    int prazoEntregaDias();
}
