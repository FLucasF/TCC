package com.loja.clube;

import com.loja.util.Arredondador;
import java.math.BigDecimal;

public class Prata implements NivelClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(PERCENTUAL_CREDITO);
        return Arredondador.arredondar(credito);
    }

    @Override
    public boolean ganhaFreteGratis() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
