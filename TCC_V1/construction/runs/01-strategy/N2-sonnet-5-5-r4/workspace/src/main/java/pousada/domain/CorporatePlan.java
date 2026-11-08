package pousada.domain;

import java.math.BigDecimal;
import java.util.Map;

/** Convênio com empresas: o desconto depende do código da empresa informado na reserva. */
public class CorporatePlan extends NightlyRatePlan {
    private final Map<String, BigDecimal> discountByCompany;

    public CorporatePlan(Map<String, BigDecimal> discountByCompany) {
        this.discountByCompany = Map.copyOf(discountByCompany);
    }

    @Override public String code() { return "CORPORATE"; }

    @Override
    public void validate(Booking booking) {
        if (booking.companyCode() == null || !discountByCompany.containsKey(booking.companyCode())) {
            throw ApiException.invalidRequest("Código de empresa ausente ou desconhecido para o plano CORPORATE");
        }
    }

    @Override protected BigDecimal discountRate(Booking booking) { return discountByCompany.get(booking.companyCode()); }
    @Override public BigDecimal refund(BigDecimal amountPaid) { return fullRefund(amountPaid); }
}
