package com.loja.dominio;

import java.util.List;

public record DadosCompra(
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
