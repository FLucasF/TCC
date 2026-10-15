package com.loja.checkout.service.clube;

import com.loja.checkout.util.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class PrataNivel implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.02");

    @Override
    public String getCodigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Money.round(subtotalProdutos.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public boolean isFreteGratis() {
        return false;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
