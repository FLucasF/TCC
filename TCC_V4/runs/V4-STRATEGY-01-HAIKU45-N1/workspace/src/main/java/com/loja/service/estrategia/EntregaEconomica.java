package com.loja.service.estrategia;

import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class EntregaEconomica implements CalculoEntrega {
    private static final BigDecimal TAXA_BASE = new BigDecimal("12.00");
    private static final BigDecimal TAXA_POR_KG = new BigDecimal("2.00");
    private static final Integer PRAZO = 7;

    @Override
    public BigDecimal calcularFrete(BigDecimal pesoTotal) {
        BigDecimal frete = TAXA_BASE.add(pesoTotal.multiply(TAXA_POR_KG));
        return Arredondador.arredondarParaCentavos(frete);
    }

    @Override
    public Integer getPrazo() {
        return PRAZO;
    }

    @Override
    public boolean disponivel(BigDecimal pesoTotal) {
        return true;
    }
}
