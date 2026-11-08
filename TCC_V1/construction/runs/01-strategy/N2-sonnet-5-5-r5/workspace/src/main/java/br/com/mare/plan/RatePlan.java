package br.com.mare.plan;

import java.math.BigDecimal;

/** Um plano de tarifa: quem pode usar, como o preço é calculado e quanto volta no cancelamento. */
public interface RatePlan {
    String code();

    /** Valida as regras do plano e calcula o preço; lança INVALID_REQUEST se o pedido não puder usar o plano. */
    Quote quote(Booking booking);

    /** Valor devolvido ao cancelar, dado o que foi pago. */
    BigDecimal refund(BigDecimal amountPaid);
}
