package com.loja.checkout.dominio.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/**
 * Ganha 5% dos produtos de volta em crédito, não paga frete nunca, e se os
 * produtos passarem de R$ 500,00 a loja manda um brinde junto.
 */
@Component
public class NivelOuro implements NivelClube {

    private static final BigDecimal PERCENTUAL = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.centavos(subtotalProdutos.multiply(PERCENTUAL));
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
