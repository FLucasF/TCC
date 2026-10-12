package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import com.loja.checkout.domain.ModalidadeEntrega;

public class EntregaRetiradaLoja implements EstrategiaEntrega {
    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return BigDecimal.ZERO;
    }

    @Override
    public int prazoEntregaDias() {
        return 1;
    }

    @Override
    public ModalidadeEntrega modalidade() {
        return ModalidadeEntrega.RETIRADA_LOJA;
    }
}
