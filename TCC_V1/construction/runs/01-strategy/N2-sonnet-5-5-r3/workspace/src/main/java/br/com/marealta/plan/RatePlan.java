package br.com.marealta.plan;

import br.com.marealta.domain.Stay;

import java.math.BigDecimal;

/** Um plano de tarifa: como precifica, quem pode usar e quanto devolve no cancelamento. */
public interface RatePlan {
    /** Nome do plano no sistema (ex.: STANDARD). */
    String code();

    /** Calcula o preço; lança INVALID_REQUEST se a estadia não puder usar o plano. */
    Quote quote(Stay stay);

    /** Quanto devolver no cancelamento, dado o valor já pago. */
    BigDecimal refund(BigDecimal amountPaid);
}
