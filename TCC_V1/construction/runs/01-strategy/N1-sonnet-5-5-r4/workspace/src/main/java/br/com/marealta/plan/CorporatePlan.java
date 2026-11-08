package br.com.marealta.plan;

import br.com.marealta.ApiError;
import br.com.marealta.Money;
import br.com.marealta.Stay;
import java.math.BigDecimal;
import java.util.Map;

/** Convênio com empresas: desconto nas diárias conforme a empresa informada. */
public class CorporatePlan extends RatePlan {
    private static final Map<String, BigDecimal> DISCOUNT_BY_COMPANY = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    public CorporatePlan() {
        super("CORPORATE");
    }

    @Override
    protected void validate(Stay stay) {
        if (stay.companyCode() == null || !DISCOUNT_BY_COMPANY.containsKey(stay.companyCode())) {
            throw ApiError.invalidRequest("O convênio exige um companyCode de empresa conveniada");
        }
    }

    @Override
    protected BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.fraction(subtotal, DISCOUNT_BY_COMPANY.get(stay.companyCode()));
    }
}
