package com.loja.domain.entrega;

import java.math.BigDecimal;

public interface ModalidadeEntrega {
    boolean aceita(BigDecimal pesoTotal);
    ResultadoFrete calcularFrete(BigDecimal pesoTotal);
}
