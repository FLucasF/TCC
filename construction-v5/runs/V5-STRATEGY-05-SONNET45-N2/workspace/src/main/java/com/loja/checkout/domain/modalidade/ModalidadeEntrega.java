package com.loja.checkout.domain.modalidade;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    BigDecimal calcularFrete(BigDecimal pesoTotal);
    int getPrazoEntregaDias();
    boolean estaDisponivel(BigDecimal pesoTotal);
}
