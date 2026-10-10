package com.loja.checkout.domain.nivel;

import com.loja.checkout.domain.NivelClube;
import java.math.BigDecimal;

import static com.loja.checkout.util.Arredondamento.arredondar;

public class Ouro implements NivelClube {

    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
    }

    @Override
    public boolean temFreteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
