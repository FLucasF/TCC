package com.loja.checkout.domain.entrega;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class EntregaExpressa implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("25.00");
        BigDecimal porKg = new BigDecimal("4.50");
        BigDecimal frete = base.add(porKg.multiply(pesoTotal));
        return Arredondamento.arredondar(frete);
    }

    @Override
    public int obterPrazo() {
        return 2;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return true;
    }
}
