package com.loja.checkout.clube;

import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL_CREDITO = new BigDecimal("5");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal percentualCredito() {
        return PERCENTUAL_CREDITO;
    }

    @Override
    public BigDecimal frete(BigDecimal freteModalidade) {
        return Dinheiro.ZERO;
    }

    /** Brinde quando os produtos passam de R$ 500,00. */
    @Override
    public boolean daBrinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
