package br.com.loja.checkout.entrega;

import br.com.loja.checkout.resumo.Carrinho;
import java.math.BigDecimal;

/** Uma opção de entrega: cada uma tem seu jeito de cobrar, seu prazo e suas limitações. */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    default boolean atende(Carrinho carrinho) {
        return true;
    }

    BigDecimal frete(Carrinho carrinho);
}
