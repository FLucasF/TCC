package com.loja.checkout.service.clube;

import com.loja.checkout.enums.NivelClube;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OuroBeneficioClube implements BeneficioClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal SUBTOTAL_MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public NivelClube getNivel() {
        return NivelClube.OURO;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO);
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(SUBTOTAL_MINIMO_BRINDE) > 0;
    }
}
