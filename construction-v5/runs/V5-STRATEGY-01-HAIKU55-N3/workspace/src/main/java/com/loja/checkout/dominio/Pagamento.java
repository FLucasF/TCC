package com.loja.checkout.dominio;

import java.math.BigDecimal;

public record Pagamento(BigDecimal totalFinal, BigDecimal valorParcela, int parcelas) {

    static Pagamento avista(BigDecimal totalFinal) {
        return new Pagamento(totalFinal, totalFinal, 1);
    }
}
