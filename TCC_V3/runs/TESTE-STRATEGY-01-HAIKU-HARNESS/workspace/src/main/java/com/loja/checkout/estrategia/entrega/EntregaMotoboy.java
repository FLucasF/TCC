package com.loja.checkout.estrategia.entrega;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class EntregaMotoboy implements EstrategiaEntrega {
    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        return Arredondamento.arredondarMeioParaPar(new BigDecimal("18.00"));
    }

    @Override
    public int getPrazo() {
        return 0;
    }

    @Override
    public void validar(BigDecimal pesoTotal) throws IllegalArgumentException {
        if (pesoTotal.compareTo(new BigDecimal("5.00")) > 0) {
            throw new IllegalArgumentException("MODALIDADE_INDISPONIVEL");
        }
    }
}
