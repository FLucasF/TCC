package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/** Uma opção de entrega. Para incluir uma nova, basta criar outro componente que implemente esta interface. */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    BigDecimal frete(Carrinho carrinho);

    default boolean atende(Carrinho carrinho) {
        return true;
    }
}
