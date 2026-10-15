package br.com.loja.checkout.entrega;

import br.com.loja.checkout.dominio.Carrinho;
import br.com.loja.checkout.dominio.Codificado;

/** Opção de entrega. Cada uma decide se atende o pedido, quanto cobra e em quanto tempo entrega. */
public interface ModalidadeEntrega extends Codificado {

    boolean atende(Carrinho carrinho);

    Cotacao cotar(Carrinho carrinho);
}
