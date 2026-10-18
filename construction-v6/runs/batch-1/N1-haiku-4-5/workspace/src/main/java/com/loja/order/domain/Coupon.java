package com.loja.order.domain;

import java.math.BigDecimal;
import java.util.Optional;

public enum Coupon {
  WELCOME10 {
    @Override
    public Optional<BigDecimal> calculateDiscount(BigDecimal productsSubtotal, OrderContext ctx) {
      if (!ctx.isFirstPurchase) return Optional.empty();
      return Optional.of(productsSubtotal.multiply(new BigDecimal("0.10")));
    }
  },
  MINUS50 {
    @Override
    public Optional<BigDecimal> calculateDiscount(BigDecimal productsSubtotal, OrderContext ctx) {
      if (productsSubtotal.compareTo(new BigDecimal("300.00")) < 0) return Optional.empty();
      return Optional.of(new BigDecimal("50.00"));
    }
  },
  FREESHIPPING {
    @Override
    public Optional<BigDecimal> calculateDiscount(BigDecimal productsSubtotal, OrderContext ctx) {
      if (ctx.shippingMethod == ShippingMethod.STORE_PICKUP) return Optional.empty();
      return Optional.of(ctx.shippingCost);
    }
  },
  BUY3PAY2 {
    @Override
    public Optional<BigDecimal> calculateDiscount(BigDecimal productsSubtotal, OrderContext ctx) {
      BigDecimal discount = BigDecimal.ZERO;
      for (OrderItem item : ctx.items) {
        int freeItems = item.quantity() / 3;
        discount = discount.add(
          new BigDecimal(freeItems).multiply(
            item.price()
          )
        );
      }
      return Optional.of(discount);
    }
  };

  public abstract Optional<BigDecimal> calculateDiscount(BigDecimal productsSubtotal, OrderContext ctx);

  public static Optional<Coupon> fromCode(String code) {
    if (code == null || code.isEmpty()) return Optional.empty();
    try {
      return Optional.of(Coupon.valueOf(code));
    } catch (IllegalArgumentException e) {
      return Optional.empty();
    }
  }

  public record OrderContext(
    java.util.List<OrderItem> items,
    boolean isFirstPurchase,
    ShippingMethod shippingMethod,
    BigDecimal shippingCost
  ) {}

  public record OrderItem(
    String sku,
    int quantity,
    BigDecimal price
  ) {}
}
