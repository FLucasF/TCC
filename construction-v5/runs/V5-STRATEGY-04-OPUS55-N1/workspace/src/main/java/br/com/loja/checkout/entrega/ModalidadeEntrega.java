package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Codificado;
import br.com.loja.checkout.carrinho.Carrinho;

import java.math.BigDecimal;

/** Uma opção de entrega. Para incluir uma nova, basta criar outra implementação anotada com @Component. */
public interface ModalidadeEntrega extends Codificado {

    /** Se esta opção pode levar o pedido. */
    default boolean atende(Carrinho carrinho) {
        return true;
    }

    /** Frete, já arredondado para centavos. */
    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
