package com.loja.domain.clube;

import com.loja.util.Moeda;
import java.math.BigDecimal;

public class Ouro implements NivelClube {
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public Beneficios calcularBeneficios(BigDecimal subtotalProdutos) {
        BigDecimal credito = Moeda.arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
        boolean brinde = subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
        return new Beneficios(credito, true, brinde);
    }
}
