package com.marealta.plan;

import java.math.BigDecimal;

/**
 * Um plano de tarifa: define o preço, quem pode usá-lo e quanto se devolve no cancelamento.
 * Para criar um plano novo basta implementar esta interface e registrá-la em {@link RatePlans}.
 */
public interface RatePlan {
    /** Nome usado na API (ex.: STANDARD, CORPORATE). */
    String code();

    /** Calcula o preço; lança {@code ApiException.invalid} se o plano não puder ser usado. */
    Quote quote(BookingTerms terms);

    /** Valor devolvido no cancelamento, dado o que foi pago (zero se estava pendente). */
    BigDecimal refund(BigDecimal amountPaid);
}
