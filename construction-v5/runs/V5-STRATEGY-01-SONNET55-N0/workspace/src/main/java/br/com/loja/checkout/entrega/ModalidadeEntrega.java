package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/** Para uma nova opcao de entrega, basta criar um @Component que implemente esta interface. */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    default boolean atende(Carrinho carrinho) {
        return true;
    }

    BigDecimal frete(Carrinho carrinho);
}
