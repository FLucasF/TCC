package com.loja.checkout.domain.coupon;

import com.loja.checkout.domain.CartItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class CouponRegistry {

    private static final Coupon BEMVINDO10 = new Coupon() {
        @Override
        public BigDecimal discount(BigDecimal subtotal, List<CartItem> items, BigDecimal frete) {
            return subtotal.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_EVEN);
        }
        @Override
        public boolean isApplicable(BigDecimal subtotal, List<CartItem> items) { return true; }
    };

    private static final Coupon MENOS50 = new Coupon() {
        private static final BigDecimal MINIMUM = new BigDecimal("300.00");
        private static final BigDecimal DISCOUNT = new BigDecimal("50.00");
        @Override
        public BigDecimal discount(BigDecimal subtotal, List<CartItem> items, BigDecimal frete) {
            return DISCOUNT;
        }
        @Override
        public boolean isApplicable(BigDecimal subtotal, List<CartItem> items) {
            return subtotal.compareTo(MINIMUM) >= 0;
        }
    };

    private static final Coupon FRETEGRATIS = new Coupon() {
        @Override
        public BigDecimal discount(BigDecimal subtotal, List<CartItem> items, BigDecimal frete) {
            return frete;
        }
        @Override
        public boolean isApplicable(BigDecimal subtotal, List<CartItem> items) { return true; }
    };

    private static final Coupon LEVE3PAGUE2 = new Coupon() {
        @Override
        public BigDecimal discount(BigDecimal subtotal, List<CartItem> items, BigDecimal frete) {
            BigDecimal total = BigDecimal.ZERO;
            for (CartItem item : items) {
                int freeUnits = item.quantidade() / 3;
                if (freeUnits > 0) {
                    total = total.add(item.precoUnitario().multiply(BigDecimal.valueOf(freeUnits)));
                }
            }
            return total.setScale(2, RoundingMode.HALF_EVEN);
        }
        @Override
        public boolean isApplicable(BigDecimal subtotal, List<CartItem> items) { return true; }
    };

    private static final Map<String, Coupon> REGISTRY = Map.of(
            "BEMVINDO10", BEMVINDO10,
            "MENOS50", MENOS50,
            "FRETEGRATIS", FRETEGRATIS,
            "LEVE3PAGUE2", LEVE3PAGUE2
    );

    private CouponRegistry() {}

    public static Optional<Coupon> find(String code) {
        return Optional.ofNullable(REGISTRY.get(code));
    }
}
