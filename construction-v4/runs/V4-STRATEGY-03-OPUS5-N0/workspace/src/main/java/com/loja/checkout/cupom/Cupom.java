package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/**
 * Uma promocao. Para o marketing lancar um cupom novo, basta criar um bean
 * que implemente esta interface: o codigo passa a ser aceito automaticamente.
 */
public interface Cupom {

    /** Codigo do cupom, sempre em letras maiusculas (ex.: BEMVINDO10). */
    String codigo();

    /** Desconto do cupom para este pedido, em centavos. */
    BigDecimal calcularDesconto(Carrinho carrinho, BigDecimal frete);

    /** Se false, o cupom existe mas o pedido nao cumpre a condicao (CUPOM_NAO_APLICAVEL). */
    default boolean aplicavel(Carrinho carrinho, BigDecimal frete) {
        return true;
    }
}
