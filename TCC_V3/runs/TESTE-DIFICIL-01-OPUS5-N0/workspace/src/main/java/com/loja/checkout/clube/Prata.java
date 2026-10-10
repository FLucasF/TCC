package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** PRATA: 2% dos produtos de volta em credito. */
@Component
public class Prata implements NivelClube {

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal percentualCredito() {
        return new BigDecimal("2");
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public int parcelasSemJuros() {
        return 3;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
