package com.loja.checkout.strategy;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    String codigo();
    BigDecimal calcularFrete(double pesoKgTotal);
    int prazoEntregaDias();
    boolean aceita(double pesoKgTotal);
}
