package com.loja.modalidade;

import com.loja.domain.Pedido;
import java.math.BigDecimal;

public interface ModalidadeEntrega {
    boolean aceita(Pedido pedido);
    BigDecimal calcularFrete(Pedido pedido);
    int prazoEmDias();
}
