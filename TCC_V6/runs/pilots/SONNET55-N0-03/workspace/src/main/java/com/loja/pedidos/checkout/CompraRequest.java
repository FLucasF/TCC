package com.loja.pedidos.checkout;

import java.util.List;

/** Dados da compra como chegam do site; tudo textual para que valores inválidos virem erros de negócio. */
public record CompraRequest(
        ClienteDados cliente,
        List<ItemDados> itens,
        String cep,
        String regiao,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube) {

    public record ClienteDados(String cpf, String email) {
    }

    public record ItemDados(String sku, Integer quantidade) {
    }
}
