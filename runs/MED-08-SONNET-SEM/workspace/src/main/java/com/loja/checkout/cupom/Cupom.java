package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Ponto de extensao: cada nova promocao vira uma implementacao desta
 * interface, sem alterar o restante do sistema.
 */
public interface Cupom {

    String codigo();

    boolean aplicavel(CupomContexto contexto);

    BigDecimal calcularDesconto(CupomContexto contexto);
}
