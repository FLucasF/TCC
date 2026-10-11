package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Uma opção de entrega. Para criar uma nova, basta escrever uma classe que implemente
 * esta interface e anotá-la com {@code @Component}: ela passa a ser aceita automaticamente.
 */
public interface ModalidadeEntrega {

    /** Código enviado pelo site em {@code modalidadeEntrega}. */
    String codigo();

    int prazoDias();

    /** Se a opção atende este pedido (ex.: limite de peso). */
    default boolean atende(Pedido pedido) {
        return true;
    }

    /** Valor do frete, já arredondado para centavos. */
    BigDecimal frete(Pedido pedido);
}
