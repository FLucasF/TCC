package br.com.marealta.plan;

import br.com.marealta.ApiException;

import java.util.Map;

/** Convênio com empresas: desconto conforme a empresa; reembolso integral. */
public class CorporatePlan extends PercentagePlan {
    private static final Map<String, Integer> COMPANIES = Map.of(
            "TECNOSUL", 12,
            "AGROVALE", 18,
            "FARMAVIDA", 25);

    public CorporatePlan() {
        super("CORPORATE", 0, true);
    }

    @Override
    public void validate(Booking booking) {
        String c = booking.companyCode();
        if (c == null || !COMPANIES.containsKey(c)) {
            throw ApiException.invalid("O plano CORPORATE exige um companyCode de empresa conveniada.");
        }
    }

    @Override
    protected int discountPercent(Booking booking) {
        return COMPANIES.get(booking.companyCode());
    }

    @Override
    public String companyCodeToStore(Booking booking) {
        return booking.companyCode();
    }
}
