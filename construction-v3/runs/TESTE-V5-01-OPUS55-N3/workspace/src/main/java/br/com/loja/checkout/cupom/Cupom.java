package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface Cupom extends Codificado {

    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** @param frete o frete que o cliente pagaria, já com as vantagens do clube */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
