package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Carrinho;
import br.com.loja.checkout.Codificado;
import java.math.BigDecimal;

public interface ModalidadeEntrega extends Codificado {

    default boolean atende(Carrinho carrinho) {
        return true;
    }

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
