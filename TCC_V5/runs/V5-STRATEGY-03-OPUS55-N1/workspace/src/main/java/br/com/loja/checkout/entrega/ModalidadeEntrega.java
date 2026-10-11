package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Codificado;
import java.math.BigDecimal;

/** Uma opção de entrega: cada uma tem seu jeito de cobrar, seu prazo e suas limitações. */
public interface ModalidadeEntrega extends Codificado {

    boolean atende(Carrinho carrinho);

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
