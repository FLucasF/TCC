package br.com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;

final class BemVindo10 implements Cupom {

    private static final BigDecimal PERCENTUAL = Dinheiro.de("10");

    @Override
    public String codigo() {
        return "BEMVINDO10";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.percentual(PERCENTUAL, contexto.subtotalProdutos());
    }
}
