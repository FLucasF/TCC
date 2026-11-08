package pousada.plan;

import java.math.BigDecimal;

/**
 * Um plano de tarifa. Cada plano decide sozinho: quem pode usá-lo (validate),
 * como o preço é calculado (quote) e quanto volta quando a reserva paga é cancelada (refund).
 * Para criar um plano novo basta implementar esta interface e registrá-lo em {@link RatePlans}.
 */
public interface RatePlan {
    /** Nome usado na API e no banco, ex.: "STANDARD". */
    String code();

    /** Lança ApiException (400 INVALID_REQUEST) se a reserva não pode usar este plano. */
    default void validate(BookingContext ctx) {}

    Quote quote(BookingContext ctx);

    /** Valor devolvido ao cancelar uma reserva em que foi pago {@code amountPaid}. */
    BigDecimal refund(BigDecimal amountPaid);
}
