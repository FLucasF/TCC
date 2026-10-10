package br.com.loja.checkout.entrega;

import br.com.loja.checkout.pedido.Carrinho;
import br.com.loja.checkout.pedido.Opcao;
import java.math.BigDecimal;

/** Uma forma de entrega: como cobra, em quanto tempo entrega e quais pedidos atende. */
public interface ModalidadeEntrega extends Opcao {

    default boolean atende(Carrinho carrinho) {
        return true;
    }

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
