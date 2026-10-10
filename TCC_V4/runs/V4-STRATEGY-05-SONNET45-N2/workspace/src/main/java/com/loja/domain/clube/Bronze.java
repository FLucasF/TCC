package com.loja.domain.clube;

import java.math.BigDecimal;

public class Bronze implements NivelClube {
    @Override
    public Beneficios calcularBeneficios(BigDecimal subtotalProdutos) {
        return new Beneficios(new BigDecimal("0.00"), false, false);
    }
}
