package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

/** O pedido em que o cliente nao usou cupom. */
public final class SemCupom implements Cupom {

    public static final SemCupom INSTANCIA = new SemCupom();

    private SemCupom() {
    }

    @Override
    public String codigo() {
        return "";
    }

    @Override
    public BigDecimal desconto(ContextoCupom contexto) {
        return Dinheiro.ZERO;
    }
}
