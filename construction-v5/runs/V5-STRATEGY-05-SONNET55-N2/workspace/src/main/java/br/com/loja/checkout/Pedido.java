package br.com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

public record Pedido(List<ItemCarrinho> itens, BigDecimal subtotal, BigDecimal pesoKg) {

    public static Pedido de(List<ItemCarrinho> itens) {
        BigDecimal subtotal = itens.stream().map(ItemCarrinho::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal peso = itens.stream().map(ItemCarrinho::pesoTotalKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new Pedido(itens, Dinheiro.arredondar(subtotal), peso);
    }
}
