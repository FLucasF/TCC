package com.loja.checkout.cupom;

import com.loja.checkout.comum.Codificado;
import java.math.BigDecimal;

/** Uma promoção: em que pedidos vale e quanto desconta. */
public interface Cupom extends Codificado {

    boolean aplicavel(ContextoCupom contexto);

    /** Desconto já arredondado para centavos. */
    BigDecimal desconto(ContextoCupom contexto);
}
