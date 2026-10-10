package br.com.loja.checkout.clube;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/** 5% dos produtos em crédito, frete grátis sempre e brinde quando os produtos passam de R$ 500,00. */
@Component
public class Ouro implements NivelClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal MINIMO_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal taxaCredito() {
        return TAXA_CREDITO;
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
