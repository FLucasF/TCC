package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/**
 * Uma opção de entrega. Para criar uma nova, basta implementar esta interface
 * em uma classe anotada com {@code @Component}.
 */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    BigDecimal frete(Carrinho carrinho);

    /** Se a opção consegue levar este pedido (limite de peso, por exemplo). */
    default boolean atende(Carrinho carrinho) {
        return true;
    }
}
