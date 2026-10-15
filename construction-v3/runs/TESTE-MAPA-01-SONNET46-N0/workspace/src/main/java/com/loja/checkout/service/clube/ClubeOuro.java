package com.loja.checkout.service.clube;

import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ClubeOuro implements EstrategiaClube {

    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String getCodigo() {
        return "OURO";
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotal) {
        return subtotal.multiply(new BigDecimal("0.05")).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean temBrinde(BigDecimal subtotal) {
        return subtotal.compareTo(LIMITE_BRINDE) > 0;
    }
}
