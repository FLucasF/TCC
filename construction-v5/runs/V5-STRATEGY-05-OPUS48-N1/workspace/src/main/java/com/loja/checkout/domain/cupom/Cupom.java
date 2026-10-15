package com.loja.checkout.domain.cupom;

import com.loja.checkout.domain.comum.ComCodigo;

import java.math.BigDecimal;

/**
 * Uma promocao. O marketing inventa cupom o tempo todo, entao cada um mora na
 * sua classe: diz se o pedido cumpre a condicao e quanto desconta.
 */
public interface Cupom extends ComCodigo {

    /** Se o pedido cumpre a condicao do cupom. Por padrao, sempre aplica. */
    default boolean aplicavel(CupomContexto contexto) {
        return true;
    }

    /** Desconto bruto; o servico arredonda para centavos. */
    BigDecimal desconto(CupomContexto contexto);
}
