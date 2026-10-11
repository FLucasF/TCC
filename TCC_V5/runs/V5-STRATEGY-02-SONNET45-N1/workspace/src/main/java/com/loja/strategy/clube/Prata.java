package com.loja.strategy.clube;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class Prata implements NivelClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Arredondamento.arredondar(
            subtotalProdutos.multiply(new BigDecimal("0.02"))
        );
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
