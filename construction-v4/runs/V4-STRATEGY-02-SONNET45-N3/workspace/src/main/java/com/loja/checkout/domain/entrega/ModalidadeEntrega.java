package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(double pesoTotalKg);
    int obterPrazo();
    boolean aceitaPedido(double pesoTotalKg);
}
