package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Codificado;
import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Cada parceria nova entra como uma implementacao,
 * com seu jeito de cobrar, seu prazo e suas limitacoes.
 */
public interface ModalidadeEntrega extends Codificado {

    BigDecimal frete(Pedido pedido);

    int prazoDias();

    /** Se esta opcao atende o pedido (ex.: limite de peso). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
