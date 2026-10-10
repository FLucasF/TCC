package com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component("BRONZE")
public class BronzeBeneficio implements BeneficioClube {

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
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
