package br.com.mare;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um plano de tarifa: cada plano decide como calcula o preço, quem pode usá-lo
 * e quanto devolve no cancelamento. Para um plano novo, basta implementar esta
 * interface e registrá-lo em {@link RatePlans}.
 */
public interface RatePlan {
    String code();

    /**
     * Calcula soma das diárias e desconto, ou lança {@link ApiException#invalid}
     * se as regras do plano não permitem esta reserva.
     */
    Quote quote(Room room, LocalDate checkIn, LocalDate checkOut, String companyCode);

    /** Valor devolvido ao cancelar uma reserva com {@code amountPaid} pago. */
    BigDecimal refund(BigDecimal amountPaid);

    record Quote(BigDecimal subtotal, BigDecimal discount) {
        public BigDecimal total() { return subtotal.subtract(discount); }
    }
}
