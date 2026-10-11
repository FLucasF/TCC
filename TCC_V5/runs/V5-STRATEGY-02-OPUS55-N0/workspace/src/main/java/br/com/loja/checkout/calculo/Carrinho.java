package br.com.loja.checkout.calculo;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho validado, com os valores derivados usados pelas regras. */
public record Carrinho(List<Item> itens, BigDecimal subtotalProdutos, BigDecimal pesoKg) {

    public static Carrinho de(List<Item> itens) {
        BigDecimal subtotal = itens.stream().map(Item::valorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream().map(Item::pesoTotalKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Carrinho(List.copyOf(itens), Dinheiro.centavos(subtotal), peso);
    }
}
