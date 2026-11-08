package br.com.marealta.plan;

import java.math.BigDecimal;

/**
 * Um plano de tarifa: como calcula o preço, quem pode usá-lo e quanto devolve no cancelamento.
 * Para criar um plano novo, implemente esta interface e registre em {@link RatePlans}.
 */
public interface RatePlan {
    /** Nome do plano na API (ex.: STANDARD). */
    String code();

    /** Calcula o preço; lança {@code ApiException.invalid} se o plano não puder ser usado nesta reserva. */
    Quote quote(PricingContext ctx);

    /** Quanto devolver no cancelamento, dado o que foi pago (já em centavos). */
    BigDecimal refund(BigDecimal amountPaid);
}
