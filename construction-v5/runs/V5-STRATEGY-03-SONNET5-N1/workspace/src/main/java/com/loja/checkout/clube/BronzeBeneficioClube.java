package com.loja.checkout.clube;

import com.loja.checkout.dominio.NivelClube;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BronzeBeneficioClube implements BeneficioClube {

    @Override
    public NivelClube nivel() {
        return NivelClube.BRONZE;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO.setScale(2);
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
