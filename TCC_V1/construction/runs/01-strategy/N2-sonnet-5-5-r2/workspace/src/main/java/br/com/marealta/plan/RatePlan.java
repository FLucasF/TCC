package br.com.marealta.plan;

import br.com.marealta.domain.Quote;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;

/** Um plano de tarifa: como calcula o preço, quem pode usar e quanto devolve no cancelamento. */
public interface RatePlan {
    /** Nome do plano no sistema (STANDARD, CORPORATE...). */
    String code();

    /** Calcula o preço da estadia; lança INVALID_REQUEST se o plano não pode ser usado nela. */
    Quote quote(Stay stay);

    /** Quanto devolver ao cancelar uma reserva em que foi pago {@code amountPaid}. */
    BigDecimal refund(BigDecimal amountPaid);
}
