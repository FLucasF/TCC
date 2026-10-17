package hotel.reservations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

final class CorporateDiscounts {
    private static final Map<String, BigDecimal> DISCOUNTS = new HashMap<>();

    static {
        DISCOUNTS.put("TECNOSUL", new BigDecimal("0.12"));
        DISCOUNTS.put("AGROVALE", new BigDecimal("0.18"));
        DISCOUNTS.put("FARMAVIDA", new BigDecimal("0.25"));
    }

    static BigDecimal getDiscount(String companyCode) {
        return DISCOUNTS.get(companyCode);
    }

    static boolean isValid(String companyCode) {
        return companyCode != null && DISCOUNTS.containsKey(companyCode);
    }
}
