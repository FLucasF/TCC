package com.loja.checkout.service.clube;

import com.loja.checkout.service.Moeda;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ClubePrata implements BeneficioClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String nivel() {
        return "PRATA";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotal) {
        return Moeda.arredondar(subtotal.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public boolean freteGratis() {
        return false;
    }

    @Override
    public boolean brinde(BigDecimal subtotal) {
        return false;
    }
}
