package com.loja.checkout.dominio.cupom;

import java.math.BigDecimal;

/**
 * Uma promocao do marketing.
 *
 * Cupom novo = classe nova anotada com @Component implementando esta interface.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, sempre em maiusculas (ex.: BEMVINDO10). */
    String codigo();

    /** Se o pedido cumpre a condicao do cupom (ex.: MENOS50 a partir de R$ 300,00). */
    default boolean aplicavel(ContextoCupom contexto) {
        return true;
    }

    /** Valor do desconto, sem arredondar (o servico arredonda para centavos). */
    BigDecimal calcularDesconto(ContextoCupom contexto);
}
