package br.com.loja.checkout.pagamento;

import java.math.BigDecimal;

/** Como o pagamento fecha: o valor final da compra e quanto fica cada parcela. */
public record ResultadoPagamento(BigDecimal totalFinal, BigDecimal valorParcela) {
}
