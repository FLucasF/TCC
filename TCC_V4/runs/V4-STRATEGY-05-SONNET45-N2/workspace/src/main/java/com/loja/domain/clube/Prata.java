package com.loja.domain.clube;

import com.loja.util.Moeda;
import java.math.BigDecimal;

public class Prata implements NivelClube {
    @Override
    public Beneficios calcularBeneficios(BigDecimal subtotalProdutos) {
        BigDecimal credito = Moeda.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
        return new Beneficios(credito, false, false);
    }
}
