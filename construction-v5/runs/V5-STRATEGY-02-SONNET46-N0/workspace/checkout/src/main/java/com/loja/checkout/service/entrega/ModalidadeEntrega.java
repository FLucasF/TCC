package com.loja.checkout.service.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    String getCodigo();
    boolean aceitaPedido(double pesoTotalKg);
    BigDecimal calcularFrete(double pesoTotalKg);
    int getPrazoDias();
}
