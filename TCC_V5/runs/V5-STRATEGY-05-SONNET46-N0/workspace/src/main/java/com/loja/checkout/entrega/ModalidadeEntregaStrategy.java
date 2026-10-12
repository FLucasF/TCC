package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntregaStrategy {
    String getCodigo();
    BigDecimal calcularFrete(BigDecimal pesoTotalKg);
    int getPrazoDias();
    boolean atende(BigDecimal pesoTotalKg);
}
