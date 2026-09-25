package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma opcao de entrega da loja.
 *
 * <p>Para adicionar uma transportadora nova basta criar uma classe que implemente
 * esta interface e anota-la com {@code @Component}: ela entra no catalogo
 * automaticamente, sem mudar mais nada no servico.
 */
public interface ModalidadeEntrega {

    /** Codigo usado pelo site (ex.: EXPRESSA). */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoEntregaDias();

    /** Diz se esta opcao atende o pedido (limite de peso, regiao, etc.). */
    default boolean atende(Pedido pedido) {
        return true;
    }

    /** Valor do frete, arredondado para centavos. */
    BigDecimal frete(Pedido pedido);
}
