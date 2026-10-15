package loja.dominio;

import java.math.BigDecimal;

public record Resumo(
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

    public Resumo {
        subtotalProdutos = Dinheiro.arredondar(subtotalProdutos);
        descontoCupom = Dinheiro.arredondar(descontoCupom);
        frete = Dinheiro.arredondar(frete);
        seguro = Dinheiro.arredondar(seguro);
        ajustePagamento = Dinheiro.arredondar(ajustePagamento);
        totalFinal = Dinheiro.arredondar(totalFinal);
        valorParcela = Dinheiro.arredondar(valorParcela);
        creditoProximaCompra = Dinheiro.arredondar(creditoProximaCompra);
    }
}
