package pousada.plan;

import java.math.BigDecimal;

/**
 * Um plano de tarifa: quem pode usar, como o preço é calculado e quanto volta no cancelamento.
 * Um plano novo é uma classe nova, registrada em {@link RatePlans}.
 */
public interface RatePlan {

    /** Nome do plano na API e no banco. */
    String code();

    /** Lança INVALID_REQUEST se o pedido não puder usar este plano. */
    default void validate(Stay stay) {
    }

    /** Soma das noites; por padrão, diária do quarto com alta temporada. */
    default BigDecimal subtotal(Stay stay) {
        return stay.seasonalSubtotal();
    }

    BigDecimal discount(Stay stay, BigDecimal subtotal);

    /** Quanto volta ao hóspede, dado o que ele pagou. */
    BigDecimal refund(BigDecimal amountPaid);

    default Pricing quote(Stay stay) {
        validate(stay);
        BigDecimal subtotal = subtotal(stay);
        return Pricing.of(subtotal, discount(stay, subtotal));
    }
}
