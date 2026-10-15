package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.util.ArredondadorMeio;

public class NivelClubePrata implements NivelClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return ArredondadorMeio.arredondar(subtotalProdutos.multiply(new BigDecimal("0.02")));
    }

    @Override
    public boolean temFreteGratis() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }

    @Override
    public String nome() {
        return "PRATA";
    }

}
