package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import com.loja.checkout.domain.ModalidadeEntrega;

public class EntregaMotoboy implements EstrategiaEntrega {
    private static final BigDecimal PESO_MAXIMO = new BigDecimal("5.00");

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return pesoTotal.compareTo(PESO_MAXIMO) <= 0;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return new BigDecimal("18.00");
    }

    @Override
    public int prazoEntregaDias() {
        return 0;
    }

    @Override
    public ModalidadeEntrega modalidade() {
        return ModalidadeEntrega.MOTOBOY;
    }
}
