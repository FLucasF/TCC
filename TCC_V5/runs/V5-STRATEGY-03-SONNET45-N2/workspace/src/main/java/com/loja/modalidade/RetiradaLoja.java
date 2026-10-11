package com.loja.modalidade;

import com.loja.domain.Pedido;
import java.math.BigDecimal;

public class RetiradaLoja implements ModalidadeEntrega {

    @Override
    public boolean aceita(Pedido pedido) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return BigDecimal.ZERO.setScale(2);
    }

    @Override
    public int prazoEmDias() {
        return 1;
    }
}
