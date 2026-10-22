package br.com.marealta.plan;

import br.com.marealta.domain.Pricing;
import br.com.marealta.domain.Stay;
import java.math.BigDecimal;

/**
 * Um plano de tarifa: cada plano decide como calcula o preço, quem pode usá-lo e quanto devolve no cancelamento.
 * Para criar um plano novo, basta implementar esta interface e listá-lo em {@link RatePlans}.
 */
public interface RatePlan {

    String code();

    /** Calcula o preço da estadia; lança INVALID_REQUEST se a estadia não puder usar este plano. */
    Pricing quote(Stay stay);

    /** Quanto volta para o hóspede, dado o que ele pagou, quando cancela antes do check-in. */
    BigDecimal refund(BigDecimal amountPaid);
}
