package br.com.loja.checkout.pedido;

import java.math.BigDecimal;

public record ItemPedido(String nome, BigDecimal precoUnitario, int quantidade, BigDecimal pesoKg) {

    public BigDecimal valor() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }

    public BigDecimal peso() {
        return pesoKg.multiply(BigDecimal.valueOf(quantidade));
    }
}
