package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.util.ArredondadorMeio;

public class NivelClubeBronze implements NivelClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return ArredondadorMeio.zero();
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
        return "BRONZE";
    }

}
