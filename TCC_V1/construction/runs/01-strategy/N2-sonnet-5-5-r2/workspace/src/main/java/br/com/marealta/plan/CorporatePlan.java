package br.com.marealta.plan;

import br.com.marealta.domain.BusinessException;
import br.com.marealta.domain.Stay;

import java.math.BigDecimal;
import java.util.Map;

/** Convênio com empresas: o desconto depende da empresa, informada pelo código. */
final class CorporatePlan extends DiscountedNightsPlan {
    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    CorporatePlan() {
        super("CORPORATE");
    }

    @Override
    void checkEligible(Stay stay) {
        String company = stay.companyCode();
        if (company == null || !DISCOUNT_BY_COMPANY.containsKey(company)) {
            throw BusinessException.invalid("O plano CORPORATE exige o código de uma empresa conveniada (companyCode)");
        }
    }

    @Override
    BigDecimal discountRate(Stay stay) {
        return DISCOUNT_BY_COMPANY.get(stay.companyCode());
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
