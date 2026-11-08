package br.com.marealta.plan;

import java.math.BigDecimal;

import br.com.marealta.domain.Money;
import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Stay;

/**
 * Um plano de tarifa: o jeito de calcular o preço, quem pode usar e quanto devolver no cancelamento.
 * Plano novo = uma classe nova + uma linha em {@link RatePlans}.
 */
public interface RatePlan {
    String code();

    /** Recusa (DomainException INVALID_REQUEST) a estadia que o plano não aceita. */
    default void validate(Stay stay) {
    }

    /** Preço padrão: soma das noites menos o desconto do plano. */
    default Quote quote(Stay stay) {
        BigDecimal subtotal = stay.nightsSubtotal();
        return new Quote(subtotal, discount(stay, subtotal));
    }

    default BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.ZERO;
    }

    /** Quanto devolver, dado o que foi pago. Por padrão, tudo. */
    default BigDecimal refund(BigDecimal paid) {
        return paid;
    }
}
