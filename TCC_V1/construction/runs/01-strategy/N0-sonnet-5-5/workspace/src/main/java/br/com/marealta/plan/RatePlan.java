package br.com.marealta.plan;

import java.math.BigDecimal;

/**
 * Um plano de tarifa: cada plano decide quem pode usá-lo, como o preço é calculado
 * e quanto é devolvido no cancelamento. Para criar um plano novo, basta implementar
 * esta interface e registrá-lo em {@link RatePlans}.
 */
public interface RatePlan {
    /** Código usado na API e no banco. */
    String code();

    /** Valida as regras de uso do plano e calcula o preço; lança ApiException 400 se não puder ser usado. */
    Quote quote(QuoteRequest request);

    /** Valor devolvido no cancelamento, dado o que foi pago (zero se nada foi pago). */
    BigDecimal refund(BigDecimal amountPaid);
}
