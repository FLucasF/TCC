package br.com.loja.checkout.pedido;

import java.math.BigDecimal;

/** Um produto do carrinho como o site mandou, ainda sem conferir. */
public record ItemBruto(String nome, BigDecimal precoUnitario, Integer quantidade, BigDecimal pesoKg) {
}
