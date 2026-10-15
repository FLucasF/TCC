package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    String codigo();
    boolean aceita(BigDecimal pesoKgTotal);
    BigDecimal calcularFrete(BigDecimal pesoKgTotal);
    int prazoEntregaDias();
}
