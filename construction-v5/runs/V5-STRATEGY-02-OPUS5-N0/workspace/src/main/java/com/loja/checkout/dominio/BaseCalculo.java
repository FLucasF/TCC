package com.loja.checkout.dominio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dados do carrinho que as politicas usam para calcular: os itens, o subtotal
 * dos produtos (arredondado) e o peso total do pedido (sem arredondar).
 */
public record BaseCalculo(List<ItemPedido> itens, BigDecimal subtotalProdutos, BigDecimal pesoTotalKg) {

    public static BaseCalculo de(List<ItemPedido> itens) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal peso = BigDecimal.ZERO;
        for (ItemPedido item : itens) {
            subtotal = subtotal.add(item.totalBruto());
            peso = peso.add(item.pesoTotalKg());
        }
        return new BaseCalculo(List.copyOf(itens), Dinheiro.arredondar(subtotal), peso);
    }
}
