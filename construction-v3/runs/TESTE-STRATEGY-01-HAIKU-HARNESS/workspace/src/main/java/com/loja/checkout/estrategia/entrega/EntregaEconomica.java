package com.loja.checkout.estrategia.entrega;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class EntregaEconomica implements EstrategiaEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal frete = new BigDecimal("12.00").add(new BigDecimal("2.00").multiply(pesoTotal));
        return Arredondamento.arredondarMeioParaPar(frete);
    }

    @Override
    public int getPrazo() {
        return 7;
    }

    @Override
    public void validar(BigDecimal pesoTotal) throws IllegalArgumentException {
    }
}
