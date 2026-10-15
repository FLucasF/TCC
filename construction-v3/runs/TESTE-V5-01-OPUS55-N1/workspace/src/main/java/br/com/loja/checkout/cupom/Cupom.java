package br.com.loja.checkout.cupom;

import br.com.loja.checkout.pedido.Carrinho;
import br.com.loja.checkout.pedido.Opcao;
import java.math.BigDecimal;

/** Uma promoção: em que pedidos vale e quanto desconta. */
public interface Cupom extends Opcao {

    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** Desconto do cupom, já arredondado; recebe o frete efetivamente cobrado do cliente. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
