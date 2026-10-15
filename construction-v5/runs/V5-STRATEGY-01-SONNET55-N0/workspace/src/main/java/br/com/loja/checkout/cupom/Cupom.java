package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/** Para um novo cupom, basta criar um @Component que implemente esta interface. */
public interface Cupom {

    String codigo();

    default boolean aplicavel(Carrinho carrinho) {
        return true;
    }

    /** Desconto em reais; {@code frete} ja considera as isencoes do clube. */
    BigDecimal desconto(Carrinho carrinho, BigDecimal frete);
}
