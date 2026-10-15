package br.com.loja.checkout.aplicacao;

import br.com.loja.checkout.dominio.Dinheiro;

/** O resumo da compra que o site mostra antes de confirmar o pedido. */
public record Resumo(
        Dinheiro subtotalProdutos,
        Dinheiro descontoCupom,
        Dinheiro frete,
        int prazoEntregaDias,
        Dinheiro seguro,
        Dinheiro ajustePagamento,
        Dinheiro totalFinal,
        int parcelas,
        Dinheiro valorParcela,
        Dinheiro creditoProximaCompra,
        boolean brinde) {
}
