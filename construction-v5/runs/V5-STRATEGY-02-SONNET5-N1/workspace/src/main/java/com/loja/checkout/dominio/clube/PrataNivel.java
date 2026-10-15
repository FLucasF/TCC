package com.loja.checkout.dominio.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PrataNivel implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO);
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
