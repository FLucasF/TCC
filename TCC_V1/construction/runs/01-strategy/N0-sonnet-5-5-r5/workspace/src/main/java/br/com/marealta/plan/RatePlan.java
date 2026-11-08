package br.com.marealta.plan;

import br.com.marealta.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Um plano de tarifa: define quem pode usá-lo, como o preço é calculado e quanto
 * é devolvido no cancelamento. Um plano novo é uma nova implementação registrada
 * em {@link RatePlans}; nada mais precisa mudar.
 */
public interface RatePlan {

    /** Dados da reserva que os planos precisam para decidir e calcular. */
    record Booking(RoomType roomType, LocalDate checkIn, LocalDate checkOut, int nights, String companyCode) {}

    /** Soma das diárias e desconto, já arredondados. */
    record Price(BigDecimal subtotal, BigDecimal discount) {
        public BigDecimal total() {
            return subtotal.subtract(discount);
        }
    }

    String code();

    /** Lança ApiException (400 INVALID_REQUEST) se o plano não puder ser usado nesta reserva. */
    default void validate(Booking booking) {}

    /** @param nightsSum soma das diárias (com alta temporada) já arredondada */
    Price price(Booking booking, BigDecimal nightsSum);

    /** Valor devolvido no cancelamento, dado o que foi pago. */
    BigDecimal refund(BigDecimal amountPaid);

    /** Código da empresa a guardar na reserva (só o convênio usa). */
    default String companyCodeToStore(Booking booking) {
        return null;
    }
}
