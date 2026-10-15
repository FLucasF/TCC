package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

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
    public boolean elegivelBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
