package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("PRATA")
public class PrataBeneficio implements BeneficioClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO);
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
