package br.com.loja.pedidos.checkout.pagamento;

import java.math.BigDecimal;

public record CondicaoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {

    public static CondicaoPagamento aVista(BigDecimal totalFinal) {
        return new CondicaoPagamento(totalFinal, 1, totalFinal);
    }
}
