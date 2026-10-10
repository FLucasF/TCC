package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Codificavel;
import java.math.BigDecimal;

/**
 * Uma promocao do marketing. Vale um cupom por pedido.
 *
 * <p>Promocao nova e uma classe anotada com {@code @Component} implementando esta interface.
 */
public interface Cupom extends Codificavel {

    /** Desconto do cupom, em centavos. */
    BigDecimal calcularDesconto(DadosCupom dados);

    /** {@code false} quando o pedido nao cumpre a condicao da promocao. */
    default boolean aplicavel(DadosCupom dados) {
        return true;
    }
}
