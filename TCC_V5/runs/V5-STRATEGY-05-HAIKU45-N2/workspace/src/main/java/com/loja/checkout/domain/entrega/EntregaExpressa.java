package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.Arredondamento;

public class EntregaExpressa implements EstrategiaEntrega {
    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal frete = new BigDecimal("25.00")
            .add(pesoTotal.multiply(new BigDecimal("4.50")));
        return Arredondamento.arredondar(frete);
    }

    @Override
    public int prazoEntregaDias() {
        return 2;
    }

    @Override
    public ModalidadeEntrega modalidade() {
        return ModalidadeEntrega.EXPRESSA;
    }
}
