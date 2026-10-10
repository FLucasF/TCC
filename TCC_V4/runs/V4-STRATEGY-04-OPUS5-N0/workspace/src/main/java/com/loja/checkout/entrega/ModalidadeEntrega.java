package com.loja.checkout.entrega;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma forma de entrega da loja. Para entrar uma transportadora nova, basta criar
 * uma classe que implemente esta interface e marcar com @Component: ela e descoberta
 * sozinha pelo {@link ModalidadesEntrega}.
 */
public interface ModalidadeEntrega {

    /** Codigo que o site manda no campo modalidadeEntrega. */
    String codigo();

    /** Prazo de entrega em dias. */
    int prazoEntregaDias();

    /** Frete do pedido, antes de arredondar. */
    BigDecimal frete(Pedido pedido);

    /** Se esta modalidade atende o pedido (peso, regiao, o que a transportadora limitar). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
