package com.loja.checkout.dominio;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.entrega.ModalidadeEntrega;
import java.math.BigDecimal;
import java.util.List;

/**
 * O pedido que o cliente montou, ja com os dados validados e resolvidos.
 * O cupom e a forma de pagamento nao entram aqui porque sao resolvidos
 * depois na ordem de validacao do resumo.
 */
public record Pedido(List<Item> itens, ModalidadeEntrega modalidadeEntrega, NivelClube nivelClube, Regiao regiao) {

    public Pedido(List<Item> itens, ModalidadeEntrega modalidadeEntrega, NivelClube nivelClube, Regiao regiao) {
        this.itens = List.copyOf(itens);
        this.modalidadeEntrega = modalidadeEntrega;
        this.nivelClube = nivelClube;
        this.regiao = regiao;
    }

    /** Soma dos produtos: preco de cada item vezes a quantidade. */
    public BigDecimal subtotalProdutos() {
        return Dinheiro.centavos(itens.stream().map(Item::total).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /** Peso do pedido: soma do peso de cada item vezes a quantidade, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        return itens.stream().map(Item::peso).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
