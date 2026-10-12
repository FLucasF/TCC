package com.loja.checkout.clube;

import com.loja.checkout.dominio.NivelClube;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BronzeBeneficio implements NivelClubeBeneficio {

    @Override
    public NivelClube getTipo() {
        return NivelClube.BRONZE;
    }

    @Override
    public BigDecimal calcularCreditoBruto(BigDecimal subtotalProdutos) {
        return BigDecimal.ZERO;
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
