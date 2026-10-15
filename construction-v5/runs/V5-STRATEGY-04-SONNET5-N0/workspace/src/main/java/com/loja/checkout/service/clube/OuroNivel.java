package com.loja.checkout.service.clube;

import com.loja.checkout.util.Money;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class OuroNivel implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal SUBTOTAL_MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String getCodigo() {
        return "OURO";
    }

    @Override
    public BigDecimal calcularCredito(BigDecimal subtotalProdutos) {
        return Money.round(subtotalProdutos.multiply(PERCENTUAL_CREDITO));
    }

    @Override
    public boolean isFreteGratis() {
        return true;
    }

    @Override
    public boolean temBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(SUBTOTAL_MINIMO_BRINDE) > 0;
    }
}
