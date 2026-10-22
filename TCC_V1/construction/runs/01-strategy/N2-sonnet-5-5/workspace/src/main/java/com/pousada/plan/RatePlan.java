package com.pousada.plan;

import com.pousada.domain.Money;

import java.math.BigDecimal;

/**
 * Um plano de tarifa. Cada plano muda apenas o que é dele: quem pode usá-lo, a soma das diárias,
 * o desconto e a devolução no cancelamento. A sequência de cálculo é a mesma para todos.
 */
public interface RatePlan {

    String code();

    /** Lança ApiException (INVALID_REQUEST) quando o plano não pode ser usado nesta reserva. */
    default void validate(PricingRequest request) {
    }

    default BigDecimal subtotal(PricingRequest request) {
        return request.sumOfNights();
    }

    default BigDecimal discount(PricingRequest request, BigDecimal subtotal) {
        return Money.ZERO;
    }

    /** Quanto volta ao hóspede quando cancela, dado o que ele pagou. */
    default BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }

    default PriceQuote quote(PricingRequest request) {
        validate(request);
        BigDecimal subtotal = subtotal(request);
        BigDecimal discount = discount(request, subtotal);
        return new PriceQuote(request.nights(), subtotal, discount, subtotal.subtract(discount));
    }
}
