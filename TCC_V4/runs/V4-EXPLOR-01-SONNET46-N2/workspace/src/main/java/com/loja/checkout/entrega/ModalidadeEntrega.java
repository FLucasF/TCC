package com.loja.checkout.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(BigDecimal pesoKg);
    int prazoEntregaDias();
    boolean aceitaPedido(BigDecimal pesoKg);
}
