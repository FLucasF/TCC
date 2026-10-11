package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    boolean estaDisponivel(BigDecimal pesoTotalKg);
    BigDecimal calcularValor(BigDecimal pesoTotalKg);
    int obterPrazoDias();
}
