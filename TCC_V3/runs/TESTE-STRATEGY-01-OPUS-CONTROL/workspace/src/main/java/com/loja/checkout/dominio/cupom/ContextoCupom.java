package com.loja.checkout.dominio.cupom;

import com.loja.checkout.dominio.Pedido;
import java.math.BigDecimal;

/**
 * Dados que o cupom precisa para decidir e calcular o desconto.
 *
 * @param pedido             carrinho do cliente
 * @param subtotalProdutos   soma dos produtos, em centavos
 * @param frete              frete que aparece no resumo (ja considerando as vantagens do clube)
 */
public record ContextoCupom(Pedido pedido, BigDecimal subtotalProdutos, BigDecimal frete) {
}
