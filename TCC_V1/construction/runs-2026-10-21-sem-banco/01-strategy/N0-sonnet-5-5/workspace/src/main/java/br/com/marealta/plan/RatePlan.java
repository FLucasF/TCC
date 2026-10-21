package br.com.marealta.plan;

import br.com.marealta.domain.Quote;
import java.math.BigDecimal;

/**
 * Um plano de tarifa: define quem pode usá-lo, como o preço é calculado e quanto
 * é devolvido no cancelamento. Um novo plano é uma nova implementação, registrada
 * em {@link RatePlans}.
 */
public interface RatePlan {
    /** Nome usado na API (ex.: STANDARD). */
    String code();

    /** Valida as regras do plano e calcula o preço; lança {@link PlanRuleException} se não puder. */
    Quote quote(BookingRequest request);

    /** Valor devolvido no cancelamento, dado o que foi pago. */
    BigDecimal refund(BigDecimal amountPaid);
}
