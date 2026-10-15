package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

public record Cobranca(BigDecimal totalFinal, int parcelas, BigDecimal valorParcela) {

    public static Cobranca aVista(BigDecimal totalFinal) {
        return new Cobranca(totalFinal, 1, totalFinal);
    }
}
