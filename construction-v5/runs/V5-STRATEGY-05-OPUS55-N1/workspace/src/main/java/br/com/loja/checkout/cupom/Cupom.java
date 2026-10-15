package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Codificado;
import br.com.loja.checkout.pedido.Carrinho;

import java.math.BigDecimal;

/** Um cupom de desconto. Para incluir um novo, basta criar outro componente que implemente esta interface. */
public interface Cupom extends Codificado {

    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** @param frete frete que o cliente pagaria, já considerando as vantagens do clube */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
