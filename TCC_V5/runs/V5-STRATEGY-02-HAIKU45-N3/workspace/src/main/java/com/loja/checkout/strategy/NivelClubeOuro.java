package com.loja.checkout.strategy;

import java.math.BigDecimal;
import com.loja.checkout.util.ArredondadorMeio;

public class NivelClubeOuro implements NivelClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return ArredondadorMeio.arredondar(subtotalProdutos.multiply(new BigDecimal("0.05")));
    }

    @Override
    public boolean temFreteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(new BigDecimal("500.00")) > 0;
    }

    @Override
    public String nome() {
        return "OURO";
    }

}
