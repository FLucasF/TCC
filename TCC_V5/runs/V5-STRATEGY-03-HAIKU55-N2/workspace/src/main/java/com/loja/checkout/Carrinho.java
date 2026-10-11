package com.loja.checkout;

import java.math.BigDecimal;
import java.util.List;

import static com.loja.checkout.Dinheiro.centavos;

record Carrinho(List<Item> itens, BigDecimal subtotal, BigDecimal pesoKg) {

    static Carrinho de(List<Item> itens) {
        if (itens == null || itens.isEmpty() || itens.stream().anyMatch(Carrinho::invalido)) {
            throw new ResumoException("PEDIDO_INVALIDO");
        }
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal peso = BigDecimal.ZERO;
        for (Item item : itens) {
            BigDecimal quantidade = BigDecimal.valueOf(item.quantidade());
            subtotal = subtotal.add(centavos(item.precoUnitario().multiply(quantidade)));
            peso = peso.add(item.pesoKg().multiply(quantidade));
        }
        return new Carrinho(itens, subtotal, peso);
    }

    private static boolean invalido(Item item) {
        return item == null
                || item.precoUnitario() == null || item.precoUnitario().signum() <= 0
                || item.quantidade() <= 0
                || item.pesoKg() == null || item.pesoKg().signum() <= 0;
    }
}
