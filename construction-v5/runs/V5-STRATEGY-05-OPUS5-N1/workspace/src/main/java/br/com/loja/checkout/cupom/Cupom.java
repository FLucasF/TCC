package br.com.loja.checkout.cupom;

import br.com.loja.checkout.comum.Codificado;
import java.math.BigDecimal;

/**
 * Uma promoção: quanto ela abate e em que pedidos ela vale. Promoção nova =
 * uma classe nova que implementa isto.
 */
public interface Cupom extends Codificado {

    BigDecimal desconto(ContextoCupom contexto);

    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
