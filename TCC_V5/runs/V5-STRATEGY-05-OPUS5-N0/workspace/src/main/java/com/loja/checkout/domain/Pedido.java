package com.loja.checkout.domain;

import com.loja.checkout.clube.NivelClube;
import java.math.BigDecimal;
import java.util.List;

/**
 * Dados do pedido que as regras de entrega, cupom, clube e pagamento consultam.
 */
public record Pedido(List<Item> itens,
                     BigDecimal subtotalProdutos,
                     BigDecimal pesoTotalKg,
                     NivelClube nivelClube,
                     Regiao regiao) {

    public static Pedido de(List<Item> itens, NivelClube nivelClube, Regiao regiao) {
        BigDecimal subtotal = Dinheiro.centavos(itens.stream()
                .map(Item::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal peso = itens.stream()
                .map(Item::peso)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Pedido(List.copyOf(itens), subtotal, peso, nivelClube, regiao);
    }
}
