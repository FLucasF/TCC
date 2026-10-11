package com.loja.checkout.domain.clube;

import com.loja.checkout.domain.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** Ganha 5% dos produtos em credito, nao paga frete nunca e, acima de R$ 500,00 em produtos, ganha brinde. */
@Component
public class Ouro implements NivelClube {

    private static final BigDecimal PERCENTUAL = new BigDecimal("5");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal credito(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, PERCENTUAL);
    }

    @Override
    public BigDecimal aplicarFrete(BigDecimal frete) {
        return Dinheiro.centavos(BigDecimal.ZERO);
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_BRINDE) > 0;
    }
}
