package br.com.loja.checkout.cupom;

import br.com.loja.checkout.Carrinho;
import java.math.BigDecimal;

/** Um cupom. Para criar um novo, basta uma classe {@code @Component} implementando esta interface. */
public interface Cupom {

    String codigo();

    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** Desconto em centavos; recebe o frete já calculado (com isenções do clube). */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
