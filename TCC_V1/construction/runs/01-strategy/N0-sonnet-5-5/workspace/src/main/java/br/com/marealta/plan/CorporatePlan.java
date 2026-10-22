package br.com.marealta.plan;

import br.com.marealta.ApiException;
import java.math.BigDecimal;
import java.util.Map;

public class CorporatePlan extends AbstractRatePlan {
    private static final Map<String, String> DISCOUNTS = Map.of(
            "TECNOSUL", "0.12",
            "AGROVALE", "0.18",
            "FARMAVIDA", "0.25");

    public String code() { return "CORPORATE"; }

    public Quote quote(QuoteRequest r) {
        String company = r.companyCode();
        if (company == null || company.isBlank()) {
            throw ApiException.invalid("O plano CORPORATE exige o código da empresa (companyCode)");
        }
        String fraction = DISCOUNTS.get(company);
        if (fraction == null) {
            throw ApiException.invalid("Código de empresa desconhecido: " + company);
        }
        return withDiscount(r, fraction);
    }

    public BigDecimal refund(BigDecimal paid) { return full(paid); }
}
