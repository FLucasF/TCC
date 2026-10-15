package br.com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;

final class Menos50 implements Cupom {

    private static final BigDecimal DESCONTO = Dinheiro.de("50.00");
    private static final BigDecimal SUBTOTAL_MINIMO = Dinheiro.de("300.00");

    @Override
    public String codigo() {
        return "MENOS50";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return contexto.subtotalProdutos().compareTo(SUBTOTAL_MINIMO) >= 0;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.centavos(DESCONTO);
    }
}
