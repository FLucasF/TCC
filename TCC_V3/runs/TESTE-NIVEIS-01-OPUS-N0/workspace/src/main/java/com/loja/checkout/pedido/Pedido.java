package com.loja.checkout.pedido;

import com.loja.checkout.clube.NivelClube;
import com.loja.checkout.comum.Dinheiro;
import java.math.BigDecimal;
import java.util.List;

/** O carrinho validado, com o cliente que esta comprando. */
public record Pedido(List<ItemPedido> itens, NivelClube nivelClube, Regiao regiao) {

    public Pedido {
        itens = List.copyOf(itens);
    }

    /** Soma dos produtos (preco x quantidade), arredondada para centavos. */
    public BigDecimal subtotalProdutos() {
        BigDecimal soma = itens.stream()
                .map(ItemPedido::valorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Dinheiro.arredondar(soma);
    }

    /** Peso do pedido: soma do peso de cada item x quantidade, sem arredondar. */
    public BigDecimal pesoTotalKg() {
        return itens.stream()
                .map(ItemPedido::pesoTotalKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
