package com.loja.checkout.domain.entrega;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class EntregaEconomica implements ModalidadeEntrega {

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal base = new BigDecimal("12.00");
        BigDecimal porKg = new BigDecimal("2.00");
        BigDecimal frete = base.add(porKg.multiply(pesoTotal));
        return Arredondamento.arredondar(frete);
    }

    @Override
    public int obterPrazo() {
        return 7;
    }

    @Override
    public boolean estaDisponivel(BigDecimal pesoTotal) {
        return true;
    }
}
