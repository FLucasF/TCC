package pousada.plan;

import pousada.PousadaException;
import pousada.Stay;

import java.math.BigDecimal;
import java.util.Map;

public class CorporatePlan extends DiscountedPlan {
    private static final Map<String, String> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", "0.12",
            "AGROVALE", "0.18",
            "FARMAVIDA", "0.25");

    public String code() { return "CORPORATE"; }

    @Override
    public void validate(Stay stay) {
        if (stay.companyCode() == null || !DISCOUNT_BY_COMPANY.containsKey(stay.companyCode())) {
            throw PousadaException.invalid("O plano CORPORATE exige o código de uma empresa conveniada");
        }
    }

    protected String discountFraction(Stay stay) { return DISCOUNT_BY_COMPANY.get(stay.companyCode()); }

    public BigDecimal refund(BigDecimal amountPaid) { return amountPaid; }
}
