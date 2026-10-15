package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Para adicionar uma transportadora nova, basta criar
 * um bean que implemente esta interface: o codigo passa a ser aceito automaticamente.
 */
public interface ModalidadeEntrega {

    /** Codigo enviado pelo site (ex.: EXPRESSA). */
    String codigo();

    /** Prazo de entrega, em dias. */
    int prazoDias();

    /** Valor do frete para este carrinho, em centavos. */
    BigDecimal calcularFrete(Carrinho carrinho);

    /** Se false, a opcao existe mas nao atende este pedido (MODALIDADE_INDISPONIVEL). */
    default boolean atende(Carrinho carrinho) {
        return true;
    }
}
