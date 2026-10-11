package br.com.loja.checkout.clube;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal taxaCredito() {
        return new BigDecimal("0.05");
    }

    @Override
    public boolean freteGratis() {
        return true;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
