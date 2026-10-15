package com.loja.checkout.clube;

import com.loja.checkout.comum.Dinheiro;
import com.loja.checkout.dominio.NivelClube;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PrataBeneficioClube implements BeneficioClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public NivelClube nivel() {
        return NivelClube.PRATA;
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Dinheiro.arredondar(subtotalProdutos.multiply(PERCENTUAL_CREDITO));
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
