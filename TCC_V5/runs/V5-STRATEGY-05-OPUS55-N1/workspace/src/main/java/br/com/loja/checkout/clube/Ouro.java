package br.com.loja.checkout.clube;

import br.com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Ouro implements NivelClube {

    private static final BigDecimal MINIMO_PARA_BRINDE = new BigDecimal("500.00");

    @Override
    public String codigo() {
        return "OURO";
    }

    @Override
    public BigDecimal frete(BigDecimal freteDaEntrega) {
        return Dinheiro.ZERO;
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, "5");
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return subtotalProdutos.compareTo(MINIMO_PARA_BRINDE) > 0;
    }
}
