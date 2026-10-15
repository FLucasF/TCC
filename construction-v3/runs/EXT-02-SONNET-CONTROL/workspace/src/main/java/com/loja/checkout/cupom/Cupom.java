package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Um cupom promocional. Novos cupons sao adicionados implementando esta
 * interface e registrando um bean, sem alterar os cupons ja existentes.
 */
public interface Cupom {

    String codigo();

    boolean aplicavel(ContextoCupom contexto);

    BigDecimal calcularDesconto(ContextoCupom contexto);
}
