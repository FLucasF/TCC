package com.loja.checkout.service.clube;

import com.loja.checkout.enums.NivelClube;
import com.loja.checkout.util.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BronzeBeneficioClube implements BeneficioClube {

    @Override
    public NivelClube getNivel() {
        return NivelClube.BRONZE;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.zero();
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
