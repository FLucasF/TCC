package br.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

/** Carrinho já validado. */
public record Carrinho(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal pesoKg) {

    public record ItemCarrinho(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {
        public BigDecimal total() {
            return Dinheiro.arredondar(precoUnitario.multiply(BigDecimal.valueOf(quantidade)));
        }
    }

    public static Carrinho de(List<ItemCarrinho> itens) {
        BigDecimal subtotal = itens.stream().map(ItemCarrinho::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream()
                .map(i -> i.pesoKg().multiply(BigDecimal.valueOf(i.quantidade())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Carrinho(itens, Dinheiro.arredondar(subtotal), peso);
    }
}
