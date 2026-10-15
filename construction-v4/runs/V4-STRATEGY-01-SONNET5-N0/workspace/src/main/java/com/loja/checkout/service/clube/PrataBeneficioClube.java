package com.loja.checkout.service.clube;

import com.loja.checkout.enums.NivelClube;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PrataBeneficioClube implements BeneficioClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public NivelClube getNivel() {
        return NivelClube.PRATA;
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
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
