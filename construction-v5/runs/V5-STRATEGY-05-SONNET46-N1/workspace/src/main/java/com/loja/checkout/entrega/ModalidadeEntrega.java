package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    String codigo();
    BigDecimal calcularFrete(BigDecimal pesoTotalKg);
    int prazoEntregaDias();
    void validarDisponibilidade(BigDecimal pesoTotalKg);
}
