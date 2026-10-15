package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Uma opcao de entrega. Para colocar uma transportadora nova no ar basta criar uma
 * classe que implemente esta interface e anota-la com @Component: ela entra sozinha
 * no catalogo, com o seu jeito de cobrar, o seu prazo e as suas limitacoes.
 */
public interface ModalidadeEntrega {

    /** Codigo que o site manda em "modalidadeEntrega". */
    String codigo();

    /** Quanto custa o frete deste pedido, em centavos. */
    BigDecimal custo(Pedido pedido);

    /** Prazo prometido, em dias. */
    int prazoEntregaDias(Pedido pedido);

    /** Se esta opcao atende o pedido (peso, tamanho, etc.). */
    default boolean atende(Pedido pedido) {
        return true;
    }
}
