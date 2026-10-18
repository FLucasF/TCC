package com.loja.service;

import com.loja.domain.Money;
import com.loja.domain.Region;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class InsuranceService {

    public Money calculateInsurance(Money productsSubtotal, Region region) {
        InsuranceRate rate = getRate(region);
        Money insurance = productsSubtotal.percentage(rate.percentage());

        if (rate.maxAmount() != null && insurance.isGreaterThan(rate.maxAmount())) {
            return rate.maxAmount();
        }

        return insurance;
    }

    private InsuranceRate getRate(Region region) {
        return switch (region) {
            case SOUTHEAST -> new InsuranceRate(BigDecimal.valueOf(1.0), null);
            case SOUTH -> new InsuranceRate(BigDecimal.valueOf(1.0), null);
            case MIDWEST -> new InsuranceRate(BigDecimal.valueOf(1.5), null);
            case NORTH -> new InsuranceRate(BigDecimal.valueOf(2.5), new Money("30.00"));
            case NORTHEAST -> new InsuranceRate(BigDecimal.valueOf(2.0), new Money("30.00"));
        };
    }

    private record InsuranceRate(BigDecimal percentage, Money maxAmount) {}
}
