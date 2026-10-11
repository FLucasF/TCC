package com.loja.checkout.service.cupom;

/**
 * Uma regra de cupom. Novas promocoes entram implementando esta interface
 * e registrando um componente Spring, sem precisar tocar no restante do
 * calculo.
 */
public interface CupomStrategy {

    String getCodigo();

    boolean aplicavel(CupomContexto contexto);

    java.math.BigDecimal calcularDesconto(CupomContexto contexto);
}
