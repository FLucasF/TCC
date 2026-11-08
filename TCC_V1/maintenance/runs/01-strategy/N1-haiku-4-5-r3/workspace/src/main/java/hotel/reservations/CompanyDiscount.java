package hotel.reservations;

import java.math.BigDecimal;
import java.util.Map;

public final class CompanyDiscount {

    private static final Map<String, BigDecimal> DISCOUNTS = Map.of(
            "TECNOSUL", new BigDecimal("0.12"),
            "AGROVALE", new BigDecimal("0.18"),
            "FARMAVIDA", new BigDecimal("0.25"));

    public static BigDecimal discountFor(String companyCode) {
        return DISCOUNTS.get(companyCode);
    }

    public static boolean isValid(String companyCode) {
        return DISCOUNTS.containsKey(companyCode);
    }
}
