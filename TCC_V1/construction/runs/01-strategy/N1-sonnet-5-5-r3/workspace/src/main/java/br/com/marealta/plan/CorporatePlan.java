package br.com.marealta.plan;

import java.math.BigDecimal;
import java.util.Map;

import br.com.marealta.domain.DomainException;
import br.com.marealta.domain.Money;
import br.com.marealta.domain.Stay;

/** Convênio com empresas: desconto nas diárias conforme o código da empresa. */
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
            throw DomainException.invalid("companyCode ausente ou desconhecido para o plano CORPORATE");
        }
    }

    @Override
    public BigDecimal discount(Stay stay, BigDecimal subtotal) {
        return Money.percent(subtotal, DISCOUNT_BY_COMPANY.get(stay.companyCode()));
    }
}
