package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import java.math.BigDecimal;

/** Cada opção de entrega define seu jeito de cobrar, seu prazo e suas limitações. */
public interface ModalidadeEntrega {

    String codigo();

    int prazoDias();

    BigDecimal frete(Carrinho carrinho);

    default boolean atende(Carrinho carrinho) {
        return true;
    }
}
