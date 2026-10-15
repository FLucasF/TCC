package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

/**
 * Um cupom de desconto. Cada promoção que o marketing inventa entra como uma
 * implementação própria: sua condição de uso e seu cálculo moram só nela.
 *
 * O desconto é devolvido sem arredondar; quem chama arredonda para centavos.
 */
public interface Cupom {

    String codigo();

    BigDecimal desconto(ContextoCupom ctx);

    default boolean aplicavel(ContextoCupom ctx) {
        return true;
    }
}
