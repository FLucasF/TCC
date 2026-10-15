package br.com.loja.checkout.resumo;

import java.util.List;

/** Dados da compra como chegam do site; os códigos ainda não foram conferidos. */
public record Compra(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        int parcelas,
        String nivelClube,
        String regiao) {
}
