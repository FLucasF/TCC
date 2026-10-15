package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Dinheiro;
import br.com.loja.checkout.Pedido;

import java.math.BigDecimal;

public interface Cupom {

    /** Usado quando o cliente não informa cupom. */
    Cupom NENHUM = new Cupom() {
        @Override
        public String codigo() {
            return "";
        }

        @Override
        public BigDecimal desconto(Pedido pedido, BigDecimal frete) {
            return Dinheiro.ZERO;
        }
    };

    String codigo();

    default boolean aplicavel(Pedido pedido) {
        return true;
    }

    /** O frete já vem com as regras do clube aplicadas, como aparece no resumo. */
    BigDecimal desconto(Pedido pedido, BigDecimal frete);
}
