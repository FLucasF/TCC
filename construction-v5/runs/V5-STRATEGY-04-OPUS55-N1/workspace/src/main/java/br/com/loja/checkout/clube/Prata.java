package br.com.loja.checkout.clube;

import br.com.loja.checkout.Dinheiro;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
class Prata implements NivelClube {

    private static final BigDecimal CREDITO_PORCENTAGEM = new BigDecimal("2");

    @Override
    public String codigo() {
        return "PRATA";
    }

    @Override
    public BigDecimal frete(BigDecimal freteDaEntrega) {
        return freteDaEntrega;
    }

    @Override
    public BigDecimal creditoProximaCompra(BigDecimal subtotalProdutos) {
        return Dinheiro.percentual(subtotalProdutos, CREDITO_PORCENTAGEM);
    }

    @Override
    public boolean brinde(BigDecimal subtotalProdutos) {
        return false;
    }
}
