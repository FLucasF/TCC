package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    boolean estaDisponivel(BigDecimal pesoKg);
    BigDecimal calcularFrete(BigDecimal pesoKg);
    int obterPrazo();
}
