package br.com.loja.checkout.cupom;

import br.com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

/** Uma promocao do marketing: quando vale e quanto desconta. */
public interface Cupom extends Codificavel {

    BigDecimal desconto(ContextoCupom contexto);

    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }
}
