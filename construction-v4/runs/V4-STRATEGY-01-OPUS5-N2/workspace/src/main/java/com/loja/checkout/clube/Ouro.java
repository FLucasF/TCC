package com.loja.checkout.clube;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Ganha 5% em credito, nao paga frete nunca e, acima de R$ 500,00, leva brinde. */
@Component
public class Ouro implements NivelClube {

    private static final BigDecimal TAXA_CREDITO = new BigDecimal("0.05");
    private static final BigDecimal PRODUTOS_PARA_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, TAXA_CREDITO);
    }

    @Override
    public BigDecimal freteDevido(BigDecimal freteDaModalidade) {
        return Dinheiro.ZERO;
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(PRODUTOS_PARA_BRINDE) > 0;
    }
}
