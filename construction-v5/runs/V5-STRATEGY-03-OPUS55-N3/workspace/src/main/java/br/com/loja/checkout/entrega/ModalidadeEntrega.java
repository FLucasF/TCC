package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Carrinho;
import java.math.BigDecimal;

/** Uma opção de entrega. Para criar uma nova, basta uma classe {@code @Component} implementando esta interface. */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    default boolean atende(Carrinho carrinho) {
        return true;
    }

    BigDecimal frete(Carrinho carrinho);
}
