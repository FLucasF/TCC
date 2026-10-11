package com.loja.modalidade;

import com.loja.domain.Pedido;
import java.math.BigDecimal;

public class Motoboy implements ModalidadeEntrega {
    private static final BigDecimal LIMITE_PESO = new BigDecimal("5");

    @Override
    public boolean aceita(Pedido pedido) {
        return pedido.pesoTotal().compareTo(LIMITE_PESO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(Pedido pedido) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoEmDias() {
        return 0;
    }
}
