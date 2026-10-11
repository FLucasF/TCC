package br.com.loja.checkout.cupom;

import br.com.loja.checkout.resumo.Carrinho;
import java.math.BigDecimal;

/** Uma promoção do marketing. O frete recebido já considera as vantagens do clube. */
public interface Cupom {

    String codigo();

    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
