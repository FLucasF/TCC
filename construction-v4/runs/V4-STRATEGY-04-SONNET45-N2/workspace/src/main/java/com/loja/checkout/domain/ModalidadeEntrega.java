package com.loja.checkout.domain;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(BigDecimal pesoTotalKg);
    int obterPrazoDias();
    boolean aceita(BigDecimal pesoTotalKg);
}
