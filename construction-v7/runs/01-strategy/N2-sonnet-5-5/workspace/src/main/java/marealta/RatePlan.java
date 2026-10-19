package marealta;

import java.math.BigDecimal;

/**
 * Plano de tarifa (Strategy). Cada plano define quem pode usá-lo, como calcula
 * o preço e quanto devolve no cancelamento. Um plano novo é uma classe nova,
 * registrada em {@link RatePlans}.
 */
public interface RatePlan {
    String code();

    /** Lança INVALID_REQUEST se a estadia não pode usar este plano. */
    default void validate(Stay stay) {}

    /** @param nightsTotal soma das noites pela diária do quarto e da temporada */
    default Quote quote(Stay stay, BigDecimal nightsTotal) {
        return new Quote(nightsTotal, Money.ZERO);
    }

    /** Valor devolvido ao cancelar uma reserva com o valor pago informado. */
    BigDecimal refund(BigDecimal amountPaid);
}
