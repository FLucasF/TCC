package shop;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

record CartLine(Product product, int quantity) {
    BigDecimal total() {
        return Money.round(product.price().multiply(BigDecimal.valueOf(quantity)));
    }
}

record CouponContext(List<CartLine> lines, BigDecimal productsSubtotal, BigDecimal shippingCost,
                     ShippingMethod shippingMethod, BooleanSupplier firstPurchase) {}

/** Um cupom: cada novo cupom é uma nova implementação registrada em {@link Coupons}. */
interface Coupon {
    String code();

    /** @throws ApiException COUPON_NOT_APPLICABLE se o pedido não cumpre a condição */
    BigDecimal discount(CouponContext ctx);

    static ApiException notApplicable() {
        return ApiException.unprocessable("COUPON_NOT_APPLICABLE");
    }
}

final class Coupons {
    /** Nenhum cupom pode passar de 30% do valor dos produtos. */
    private static final String MAX_DISCOUNT_RATE = "0.30";

    private final Map<String, Coupon> byCode = new LinkedHashMap<>();

    Coupons(Coupon... coupons) {
        for (Coupon c : coupons) byCode.put(c.code(), c);
    }

    static Coupons standard() {
        return new Coupons(new Welcome10(), new Minus50(), new FreeShipping(), new Buy3Pay2());
    }

    Coupon get(String code) {
        return code == null ? null : byCode.get(code);
    }

    BigDecimal discountFor(Coupon coupon, CouponContext ctx) {
        BigDecimal discount = Money.round(coupon.discount(ctx));
        BigDecimal cap = Money.percent(ctx.productsSubtotal(), MAX_DISCOUNT_RATE);
        return discount.compareTo(cap) > 0 ? cap : discount;
    }

    static final class Welcome10 implements Coupon {
        public String code() { return "WELCOME10"; }

        public BigDecimal discount(CouponContext ctx) {
            if (!ctx.firstPurchase().getAsBoolean()) throw Coupon.notApplicable();
            return Money.percent(ctx.productsSubtotal(), "0.10");
        }
    }

    static final class Minus50 implements Coupon {
        public String code() { return "MINUS50"; }

        public BigDecimal discount(CouponContext ctx) {
            if (ctx.productsSubtotal().compareTo(new BigDecimal("300.00")) < 0) throw Coupon.notApplicable();
            return new BigDecimal("50.00");
        }
    }

    static final class FreeShipping implements Coupon {
        public String code() { return "FREESHIPPING"; }

        public BigDecimal discount(CouponContext ctx) {
            if (!ctx.shippingMethod().chargesShipping()) throw Coupon.notApplicable();
            return ctx.shippingCost();
        }
    }

    static final class Buy3Pay2 implements Coupon {
        public String code() { return "BUY3PAY2"; }

        public BigDecimal discount(CouponContext ctx) {
            BigDecimal total = BigDecimal.ZERO;
            for (CartLine line : ctx.lines()) {
                total = total.add(line.product().price().multiply(BigDecimal.valueOf(line.quantity() / 3)));
            }
            return total;
        }
    }
}
