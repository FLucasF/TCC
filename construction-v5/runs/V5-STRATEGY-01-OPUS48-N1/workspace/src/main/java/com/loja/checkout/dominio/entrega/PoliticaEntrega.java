package com.loja.checkout.dominio.entrega;

import java.math.BigDecimal;

/**
 * Uma forma de entrega. Cada parceria nova de transportadora entra como uma
 * implementação própria, com seu jeito de cobrar, seu prazo e suas limitações —
 * sem mexer nas outras nem no cálculo geral.
 *
 * O custo é devolvido sem arredondar; quem chama arredonda para centavos.
 */
public interface PoliticaEntrega {

    String codigo();

    BigDecimal custo(BigDecimal pesoKg);

    int prazoDias();

    default boolean disponivel(BigDecimal pesoKg) {
        return true;
    }
}
