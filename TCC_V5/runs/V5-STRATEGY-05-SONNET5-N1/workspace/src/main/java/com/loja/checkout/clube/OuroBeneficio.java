package com.loja.checkout.clube;

import com.loja.checkout.dominio.NivelClube;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class OuroBeneficio implements NivelClubeBeneficio {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal SUBTOTAL_MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public NivelClube getTipo() {
        return NivelClube.OURO;
    }

    @Override
    public BigDecimal calcularCreditoBruto(BigDecimal subtotalProdutos) {
        return subtotalProdutos.multiply(PERCENTUAL_CREDITO);
    }

    @Override
    public boolean isentaFrete() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(SUBTOTAL_MINIMO_BRINDE) > 0;
    }
}
