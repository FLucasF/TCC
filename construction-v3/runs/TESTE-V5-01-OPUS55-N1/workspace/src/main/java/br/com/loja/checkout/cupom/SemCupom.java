package br.com.loja.checkout.cupom;

import br.com.loja.checkout.pedido.Carrinho;
import br.com.loja.checkout.pedido.Dinheiro;
import java.math.BigDecimal;

/** Usado quando o cliente não informa cupom. */
public final class SemCupom implements Cupom {

    public static final Cupom INSTANCIA = new SemCupom();

    private SemCupom() {
    }

    @Override
    public String codigo() {
        return "";
    }

    @Override
    public BigDecimal desconto(Carrinho carrinho, BigDecimal frete) {
        return Dinheiro.ZERO;
    }
}
