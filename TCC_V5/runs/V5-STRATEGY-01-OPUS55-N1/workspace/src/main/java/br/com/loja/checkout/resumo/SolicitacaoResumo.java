package br.com.loja.checkout.resumo;

import java.util.List;

public record SolicitacaoResumo(
        List<Item> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        Integer parcelas,
        String nivelClube,
        String regiao) {
}
