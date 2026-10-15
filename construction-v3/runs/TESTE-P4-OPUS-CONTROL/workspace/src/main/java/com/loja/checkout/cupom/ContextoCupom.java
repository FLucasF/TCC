package com.loja.checkout.cupom;

import com.loja.checkout.dominio.Pedido;

import java.math.BigDecimal;

/**
 * Dados disponiveis para um cupom decidir o desconto.
 *
 * @param pedido   carrinho do cliente
 * @param subtotal soma dos produtos, em centavos
 * @param frete    frete ja calculado (inclui a isencao do clube), em centavos
 */
public record ContextoCupom(Pedido pedido, BigDecimal subtotal, BigDecimal frete) {
}
