package br.com.loja.checkout.resumo;

import java.math.BigDecimal;
import java.util.List;

/** Dados da compra enviados pelo site. */
public record SolicitacaoResumo(
        List<ItemSolicitado> itens,
        String modalidadeEntrega,
        String cupom,
        String formaPagamento,
        BigDecimal parcelas,
        String nivelClube,
        String regiao) {
}
