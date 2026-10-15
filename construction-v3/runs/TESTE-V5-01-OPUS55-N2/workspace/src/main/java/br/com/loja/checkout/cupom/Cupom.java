package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Codificado;
import java.math.BigDecimal;

/** Um cupom de desconto. Para incluir um novo, basta criar outro componente que implemente esta interface. */
public interface Cupom extends Codificado {

    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** Desconto do cupom; recebe o frete já calculado porque há cupom que desconta o frete. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
