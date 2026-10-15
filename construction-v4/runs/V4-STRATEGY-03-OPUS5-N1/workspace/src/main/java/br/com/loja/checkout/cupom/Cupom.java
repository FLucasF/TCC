package br.com.loja.checkout.cupom;

import br.com.loja.checkout.catalogo.Codificado;
import java.math.BigDecimal;

/**
 * Uma promocao. Cada promocao nova entra como uma implementacao desta
 * interface, com sua condicao e sua conta de desconto.
 */
public interface Cupom extends Codificado {

    /** Desconto em centavos. */
    BigDecimal desconto(ContextoCupom contexto);

    /** Se o pedido cumpre a condicao da promocao. */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
