package com.loja.checkout.dominio;

import java.util.List;
import java.util.Optional;

/**
 * Os dados da compra já traduzidos para o domínio. Cada método recusa o pedido
 * (lançando {@link PedidoRecusadoException}) quando o dado recebido não existe ou não veio;
 * a tradução é feita só quando o cálculo pede, para que os problemas sejam encontrados na
 * ordem combinada.
 */
public interface EntradaPedido {

    List<ItemPedido> itens();

    NivelClube nivelClube();

    Regiao regiao();

    ModalidadeEntrega modalidadeEntrega();

    Optional<Cupom> cupom();

    FormaPagamento formaPagamento();

    int parcelas();
}
