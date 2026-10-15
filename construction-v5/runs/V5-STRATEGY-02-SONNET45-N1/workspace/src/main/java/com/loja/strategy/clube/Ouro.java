package com.loja.strategy.clube;

import com.loja.util.Arredondamento;
import java.math.BigDecimal;

public class Ouro implements NivelClube {
    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Arredondamento.arredondar(
            subtotalProdutos.multiply(new BigDecimal("0.05"))
        );
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }
}
