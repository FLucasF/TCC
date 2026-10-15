package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public interface ModalidadeEntrega {

    BigDecimal calcularFrete(BigDecimal pesoTotalKg);

    int getPrazoEntregaDias();

    boolean aceitaPedido(BigDecimal pesoTotalKg);
}
