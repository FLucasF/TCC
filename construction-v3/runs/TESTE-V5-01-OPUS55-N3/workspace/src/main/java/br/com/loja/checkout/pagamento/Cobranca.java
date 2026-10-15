package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {

    static Cobranca aVista(BigDecimal totalFinal) {
        return new Cobranca(totalFinal, totalFinal);
    }
}
