package marealta;

import java.math.BigDecimal;
import java.util.Map;

public class CorporatePlan implements RatePlan {
    private static final Map<String, Integer> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", 12,
            "AGROVALE", 18,
            "FARMAVIDA", 25);

    public String code() { return "CORPORATE"; }

    @Override
    public void validate(Stay stay) {
        if (stay.companyCode() == null || !DISCOUNT_BY_COMPANY.containsKey(stay.companyCode())) {
            throw ApiException.invalid("Plano CORPORATE exige um companyCode de empresa conveniada");
        }
    }

    @Override
    public Quote quote(Stay stay, BigDecimal nightsTotal) {
        return new Quote(nightsTotal, Money.percent(nightsTotal, DISCOUNT_BY_COMPANY.get(stay.companyCode())));
    }

    public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
