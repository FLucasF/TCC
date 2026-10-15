package com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ClubePrata implements BeneficiosClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.02");

    @Override
    public String getCodigo() {
        return "PRATA";
    }

    @Override
    public boolean isFreteGratis() {
        return false;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(TAXA_CREDITO).setScale(2, RoundingMode.HALF_EVEN);
    }

    @Override
    public boolean ganhaBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
