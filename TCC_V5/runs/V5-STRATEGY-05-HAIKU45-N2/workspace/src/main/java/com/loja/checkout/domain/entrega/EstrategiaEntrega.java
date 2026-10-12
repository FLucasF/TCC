package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import com.loja.checkout.domain.ModalidadeEntrega;

public interface EstrategiaEntrega {
    boolean disponivel(BigDecimal pesoTotal);
    BigDecimal calcularFrete(BigDecimal pesoTotal);
    int prazoEntregaDias();
    ModalidadeEntrega modalidade();
}
