package com.loja.checkout.clube;

import com.loja.checkout.dominio.NivelClube;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class PrataBeneficio implements NivelClubeBeneficio {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public NivelClube getTipo() {
        return NivelClube.PRATA;
    }

    @Override
    public BigDecimal calcularCreditoBruto(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO);
    }

    @Override
    public boolean isentaFrete() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
