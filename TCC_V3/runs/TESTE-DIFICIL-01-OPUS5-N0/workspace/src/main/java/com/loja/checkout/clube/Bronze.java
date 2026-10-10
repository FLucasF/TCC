package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** BRONZE: so o cadastro, nao ganha nada. */
@Component
public class Bronze implements NivelClube {

    @Override
    public String codigo() {
        return "BRONZE";
    }

    @Override
    public BigDecimal percentualCredito() {
        return BigDecimal.ZERO;
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
