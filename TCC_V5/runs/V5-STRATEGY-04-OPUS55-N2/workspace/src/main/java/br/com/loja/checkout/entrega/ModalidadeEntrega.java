package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/** Uma opção de entrega. Cada transportadora nova entra como uma nova implementação. */
public interface ModalidadeEntrega {

    String codigo();

    /** Se a opção consegue levar este pedido. */
    default boolean atende(Carrinho carrinho) {
        return true;
    }

    /** Frete cobrado pela opção, já arredondado para centavos. */
    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
