package shop.domain.coupon;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import shop.domain.Money;

public class Coupons {
    /** Nenhum cupom pode passar de 30% do valor dos produtos. */
    private static final BigDecimal MAX_DISCOUNT_PERCENT = new BigDecimal("30");

    private final Map<String, Coupon> byCode = new LinkedHashMap<>();

    public Coupons(List<Coupon> coupons) {
        coupons.forEach(c -> byCode.put(c.code(), c));
    }

    /** Cupons de hoje; uma nova promoção entra nesta lista. */
    public static Coupons standard() {
        return new Coupons(List.of(new Welcome10(), new Minus50(), new FreeShipping(), new Buy3Pay2()));
    }

    public Optional<Coupon> find(String code) {
        return Optional.ofNullable(code).map(byCode::get);
    }

    public static BigDecimal cap(BigDecimal discount, BigDecimal productsSubtotal) {
        BigDecimal max = Money.percent(productsSubtotal, MAX_DISCOUNT_PERCENT);
        return Money.round(discount.min(max));
    }
}
