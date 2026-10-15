package br.com.loja.checkout.entrega;

import br.com.loja.checkout.resumo.Carrinho;
import br.com.loja.checkout.resumo.Codificado;
import java.math.BigDecimal;

/** Cada opção de entrega tem seu jeito de cobrar, seu prazo e suas limitações. */
public interface ModalidadeEntrega extends Codificado {

    boolean atende(Carrinho carrinho);

    BigDecimal frete(Carrinho carrinho);

    int prazoDias();
}
