package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;

import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Para criar uma opcao nova (transportadora nova, por
 * exemplo) basta acrescentar uma classe que implemente esta interface e marcar
 * com @Component: ela entra no catalogo automaticamente.
 */
public interface ModalidadeEntrega {

    /** Codigo usado pelo site, ex.: "EXPRESSA". */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoEntregaDias();

    /** Valor do frete, sem arredondar (o arredondamento fica com o servico). */
    BigDecimal frete(Carrinho carrinho);

    /** Se esta opcao atende o pedido (peso maximo, regiao, etc.). */
    default boolean atende(Carrinho carrinho) {
        return true;
    }
}
