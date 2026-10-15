package com.loja.checkout.domain.entrega;

import java.math.BigDecimal;
import com.loja.checkout.domain.ModalidadeEntrega;
import com.loja.checkout.domain.Arredondamento;

public class EntregaEconomica implements EstrategiaEntrega {
    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal frete = new BigDecimal("12.00")
            .add(pesoTotal.multiply(new BigDecimal("2.00")));
        return Arredondamento.arredondar(frete);
    }

    @Override
    public int prazoEntregaDias() {
        return 7;
    }

    @Override
    public ModalidadeEntrega modalidade() {
        return ModalidadeEntrega.ECONOMICA;
    }
}
