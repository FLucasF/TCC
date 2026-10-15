package br.com.loja.checkout.pedido;

import java.util.List;

public record PedidoRequest(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
