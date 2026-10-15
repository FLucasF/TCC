package com.loja.checkout.domain.nivel;

import com.loja.checkout.domain.NivelClube;
import java.math.BigDecimal;

import static com.loja.checkout.util.Arredondamento.arredondar;

public class Prata implements NivelClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
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
