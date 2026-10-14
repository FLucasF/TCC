package br.com.loja.pedidos.checkout;

import java.util.List;

/** Resultado do cálculo: o resumo e os itens com os preços usados nele. */
public record Checkout(DadosCompra dados, List<ItemPrecificado> itens, Resumo resumo) {
}
