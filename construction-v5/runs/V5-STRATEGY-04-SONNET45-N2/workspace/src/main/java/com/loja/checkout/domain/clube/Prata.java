package com.loja.checkout.domain.clube;

import com.loja.checkout.util.Arredondamento;
import java.math.BigDecimal;

public class Prata implements NivelClube {
    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        BigDecimal credito = subtotalProdutos.multiply(PERCENTUAL_CREDITO);
        return Arredondamento.arredondar(credito);
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
