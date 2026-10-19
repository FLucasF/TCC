package marealta;

import java.math.BigDecimal;

public class StandardPlan implements RatePlan {
    public String code() { return "STANDARD"; }
    public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
