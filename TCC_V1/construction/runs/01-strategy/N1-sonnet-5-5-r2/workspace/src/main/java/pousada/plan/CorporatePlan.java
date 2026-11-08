package pousada.plan;

import pousada.domain.AppException;
import pousada.domain.ErrorCode;
import pousada.domain.Money;

import java.math.BigDecimal;
import java.util.Map;

public class CorporatePlan implements RatePlan {
    private static final Map<String, String> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", "0.12",
            "AGROVALE", "0.18",
            "FARMAVIDA", "0.25");

    @Override
    public String code() {
        return "CORPORATE";
    }

    @Override
    public void validate(Stay stay) {
        if (stay.companyCode() == null || !DISCOUNT_BY_COMPANY.containsKey(stay.companyCode())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "O plano CORPORATE exige um companyCode válido (TECNOSUL, AGROVALE ou FARMAVIDA)");
        }
    }

    @Override
    public BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.fraction(subtotal, DISCOUNT_BY_COMPANY.get(stay.companyCode()));
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
