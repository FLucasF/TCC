package com.loja.checkout.estrategia.entrega;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class EntregaExpressa implements EstrategiaEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal frete = new BigDecimal("25.00").add(new BigDecimal("4.50").multiply(pesoTotal));
        return Arredondamento.arredondarMeioParaPar(frete);
    }

    @Override
    public int getPrazo() {
        return 2;
    }

    @Override
    public void validar(BigDecimal pesoTotal) throws IllegalArgumentException {
    }
}
