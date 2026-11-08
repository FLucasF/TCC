package br.com.marealta.plan;

import br.com.marealta.Money;
import br.com.marealta.Stay;
import java.math.BigDecimal;

/**
 * Um plano de tarifa. A sequência (validar, somar, descontar) é escrita aqui uma vez;
 * cada plano muda apenas o que é dele. Plano novo = uma classe nova + uma linha em {@link RatePlans}.
 */
public abstract class RatePlan {
    private final String code;

    protected RatePlan(String code) {
        this.code = code;
    }

    public final String code() {
        return code;
    }

    public final Quote quote(Stay stay) {
        validate(stay);
        BigDecimal subtotal = subtotal(stay);
        return new Quote(subtotal, discount(stay, subtotal));
    }

    /** Quanto volta ao hóspede quando cancela depois de pagar {@code paid}. */
    public BigDecimal refund(BigDecimal paid) {
        return paid;
    }

    /** Lança {@code ApiError.invalidRequest} se o plano não pode ser usado nesta estadia. */
    protected void validate(Stay stay) {
    }

    protected BigDecimal subtotal(Stay stay) {
        return stay.nightsTotal();
    }

    protected BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.ZERO;
    }
}
