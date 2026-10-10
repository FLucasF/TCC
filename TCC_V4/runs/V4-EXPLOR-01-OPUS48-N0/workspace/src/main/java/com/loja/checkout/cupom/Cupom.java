package com.loja.checkout.cupom;

import java.math.BigDecimal;

/**
 * Um cupom de desconto.
 *
 * <p>O marketing adora inventar promoção: cada cupom é uma implementação desta
 * interface registrada como {@code @Component}, descoberta automaticamente pelo
 * {@link CupomRegistry}. Criar um cupom novo não exige mexer no cálculo.
 */
public interface Cupom {

    /** Código do cupom, sempre em maiúsculas (ex.: "BEMVINDO10"). */
    String codigo();

    /** Diz se o pedido cumpre a condição do cupom (ex.: MENOS50 exige R$ 300,00 em produtos). */
    boolean aplicavel(CupomContexto contexto);

    /** Valor do desconto, já arredondado para centavos. Só é chamado quando {@link #aplicavel} é verdadeiro. */
    BigDecimal desconto(CupomContexto contexto);
}
