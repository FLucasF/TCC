package br.com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

import br.com.loja.checkout.dominio.Dinheiro;

final class FreteGratis implements Cupom {

    @Override
    public String codigo() {
        return "FRETEGRATIS";
    }

    @Override
    public boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.centavos(contexto.frete());
    }
}
