package br.com.loja.checkout.entrega;

import br.com.loja.checkout.Carrinho;
import java.math.BigDecimal;

public interface ModalidadeEntrega {

    String codigo();

    boolean atende(Carrinho carrinho);

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
