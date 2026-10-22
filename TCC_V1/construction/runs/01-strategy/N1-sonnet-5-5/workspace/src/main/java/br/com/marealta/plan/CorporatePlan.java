package br.com.marealta.plan;

import br.com.marealta.domain.ApiException;
import br.com.marealta.domain.Stay;
import java.math.BigDecimal;
import java.util.Map;

/** Convênio com empresas: o desconto depende do código da empresa informado na reserva. */
class CorporatePlan extends DiscountPlan {

    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", percent("12"),
            "AGROVALE", percent("18"),
            "FARMAVIDA", percent("25"));

    @Override
    public String code() {
        return "CORPORATE";
    }

    @Override
    protected BigDecimal discountRate(Stay stay) {
        String companyCode = stay.companyCode();
        if (companyCode == null || !DISCOUNT_BY_COMPANY.containsKey(companyCode)) {
            throw ApiException.invalidRequest("O plano CORPORATE exige o código de uma empresa conveniada");
        }
        return DISCOUNT_BY_COMPANY.get(companyCode);
    }

    @Override
    public BigDecimal refund(BigDecimal amountPaid) {
        return amountPaid;
    }
}
