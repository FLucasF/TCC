package com.loja.checkout.service.clube;

import com.loja.checkout.service.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ClubeOuro implements BeneficioClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal LIMITE_BRINDE = new BigDecimal("500.00");

    @Override
    public String nivel() {
        return "OURO";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotal) {
        return Moeda.arredondar(subtotal.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return subtotal.compareTo(LIMITE_BRINDE) > 0;
    }
}
