package reference;

import java.math.BigDecimal;

/** What the calculator returns: the summary, or the refusal (the error code and the HTTP status). */
public sealed interface Result permits Result.Summary, Result.Refusal {

    /** The summary, with the same names as the API's JSON. */
    record Summary(
            BigDecimal productsSubtotal,
            BigDecimal couponDiscount,
            BigDecimal shippingCost,
            int deliveryDays,
            BigDecimal insurance,
            BigDecimal paymentAdjustment,
            BigDecimal finalTotal,
            int installments,
            BigDecimal installmentAmount,
            BigDecimal nextPurchaseCredit,
            boolean gift) implements Result {}

    record Refusal(String error, int http) implements Result {}
}
