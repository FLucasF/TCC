package br.com.loja.checkout.api;

import java.math.BigDecimal;

import br.com.loja.checkout.aplicacao.Resumo;

/** O resumo como o site recebe: todo valor em dinheiro com 2 casas decimais. */
public record ResumoResponse(
        BigDecimal subtotalProdutos,
        BigDecimal descontoCupom,
        BigDecimal frete,
        int prazoEntregaDias,
        BigDecimal seguro,
        BigDecimal ajustePagamento,
        BigDecimal totalFinal,
        int parcelas,
        BigDecimal valorParcela,
        BigDecimal creditoProximaCompra,
        boolean brinde) {

    static ResumoResponse de(Resumo resumo) {
        return new ResumoResponse(
                resumo.subtotalProdutos().valor(),
                resumo.descontoCupom().valor(),
                resumo.frete().valor(),
                resumo.prazoEntregaDias(),
                resumo.seguro().valor(),
                resumo.ajustePagamento().valor(),
                resumo.totalFinal().valor(),
                resumo.parcelas(),
                resumo.valorParcela().valor(),
                resumo.creditoProximaCompra().valor(),
                resumo.brinde());
    }
}
