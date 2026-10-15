package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Como a forma de pagamento fecha a conta: valor final e valor da parcela. */
public record Cobranca(BigDecimal totalFinal, BigDecimal valorParcela) {
}
