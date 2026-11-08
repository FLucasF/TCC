package pousada.domain;

import java.math.BigDecimal;

/** Um plano de tarifa: quem pode usar, como o preço é calculado e quanto volta no cancelamento. */
public interface RatePlan {
    String code();

    /** Lança INVALID_REQUEST se a reserva não pode usar este plano. */
    void validate(Booking booking);

    Quote quote(Booking booking);

    BigDecimal refund(BigDecimal amountPaid);
}
