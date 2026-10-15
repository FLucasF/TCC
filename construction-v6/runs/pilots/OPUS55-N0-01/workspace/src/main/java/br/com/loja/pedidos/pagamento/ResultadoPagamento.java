package br.com.loja.pedidos.pagamento;

import java.math.BigDecimal;

public record ResultadoPagamento(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {

    public static ResultadoPagamento aVista(BigDecimal totalFinal) {
        return new ResultadoPagamento(totalFinal, 1, totalFinal);
    }
}
