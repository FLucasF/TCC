package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma promocao do marketing.
 *
 * <p>Para publicar um cupom novo basta criar uma classe que implemente esta interface e anota-la
 * com {@code @Component}.
 */
public interface Cupom {

    /** Codigo digitado pelo cliente, em letras maiusculas (ex.: BEMVINDO10). */
    String codigo();

    /** Se o pedido cumpre a condicao da promocao (ex.: valor minimo em produtos). */
    default boolean aplicavel(Pedido pedido, BigDecimal frete) {
        return true;
    }

    /** Desconto em reais (o servico arredonda para centavos). */
    BigDecimal calcularDesconto(Pedido pedido, BigDecimal frete);
}
