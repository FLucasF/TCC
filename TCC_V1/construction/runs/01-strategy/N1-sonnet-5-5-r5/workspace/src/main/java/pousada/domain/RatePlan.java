package pousada.domain;

import java.math.BigDecimal;

/**
 * Um plano de tarifa: o seu jeito de calcular o preço, a sua regra de quem pode usar
 * e a sua regra de devolução no cancelamento. Um plano novo é uma classe nova,
 * registrada em {@link RatePlans}.
 */
public interface RatePlan {

    String code();

    /** Lança INVALID_REQUEST se a reserva não puder usar este plano. */
    default void validate(Booking booking) {
    }

    /** Soma das noites. */
    default BigDecimal subtotal(Booking booking) {
        return booking.nightsTotal();
    }

    default BigDecimal discount(Booking booking, BigDecimal subtotal) {
        return Money.ZERO;
    }

    /** Quanto volta ao hóspede, dado o que ele pagou. */
    BigDecimal refund(BigDecimal amountPaid);

    /** A sequência comum a todos os planos: valida, soma as noites e aplica o desconto. */
    default Quote quote(Booking booking) {
        validate(booking);
        BigDecimal subtotal = subtotal(booking);
        return new Quote(subtotal, discount(booking, subtotal));
    }
}
