package com.loja.checkout.web;

import com.loja.checkout.dominio.Dinheiro;
import java.math.BigDecimal;

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

    public ResumoResponse {
        subtotalProdutos = Dinheiro.centavos(subtotalProdutos);
        descontoCupom = Dinheiro.centavos(descontoCupom);
        frete = Dinheiro.centavos(frete);
        seguro = Dinheiro.centavos(seguro);
        ajustePagamento = Dinheiro.centavos(ajustePagamento);
        totalFinal = Dinheiro.centavos(totalFinal);
        valorParcela = Dinheiro.centavos(valorParcela);
        creditoProximaCompra = Dinheiro.centavos(creditoProximaCompra);
    }
}
