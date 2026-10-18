package com.loja.order.service;

import com.loja.order.domain.*;
import com.loja.order.model.OrderSummary;
import com.loja.order.util.RoundingUtil;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;

@Service
public class PricingService {

  public OrderSummary calculateSummary(
    List<SummaryItem> items,
    String couponCode,
    ShippingMethod shippingMethod,
    BigDecimal shippingCost,
    int deliveryDays,
    Region region,
    PaymentMethod paymentMethod,
    int installments,
    ClubTier clubTier,
    boolean isFirstPurchase
  ) {
    BigDecimal productsSubtotal = calculateProductsSubtotal(items);

    BigDecimal couponDiscount = calculateCouponDiscount(
      couponCode, productsSubtotal, items, isFirstPurchase, shippingMethod, shippingCost
    );

    BigDecimal finalShippingCost = calculateShippingCost(
      shippingCost, clubTier, shippingMethod, couponCode
    );

    BigDecimal insurance = calculateInsurance(productsSubtotal, region);

    BigDecimal subtotalAfterDiscount = productsSubtotal.subtract(couponDiscount)
      .add(finalShippingCost).add(insurance);

    BigDecimal paymentAdjustment = calculatePaymentAdjustment(
      subtotalAfterDiscount, paymentMethod, installments, couponCode
    );

    BigDecimal finalTotal = subtotalAfterDiscount.add(paymentAdjustment);

    BigDecimal installmentAmount = calculateInstallmentAmount(
      finalTotal, paymentMethod, installments
    );

    BigDecimal nextPurchaseCredit = calculateCredit(
      productsSubtotal, clubTier
    );

    boolean gift = shouldGiveGift(productsSubtotal, clubTier);

    return new OrderSummary(
      RoundingUtil.roundHalfEven(productsSubtotal),
      RoundingUtil.roundHalfEven(couponDiscount),
      RoundingUtil.roundHalfEven(finalShippingCost),
      deliveryDays,
      RoundingUtil.roundHalfEven(insurance),
      RoundingUtil.roundHalfEven(paymentAdjustment),
      RoundingUtil.roundHalfEven(finalTotal),
      installments,
      RoundingUtil.roundHalfEven(installmentAmount),
      RoundingUtil.roundHalfEven(nextPurchaseCredit),
      gift
    );
  }

  private BigDecimal calculateProductsSubtotal(List<SummaryItem> items) {
    return items.stream()
      .map(item -> item.price().multiply(new BigDecimal(item.quantity())))
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal calculateCouponDiscount(
    String couponCode,
    BigDecimal productsSubtotal,
    List<SummaryItem> items,
    boolean isFirstPurchase,
    ShippingMethod shippingMethod,
    BigDecimal shippingCost
  ) {
    if (couponCode == null || couponCode.isEmpty()) {
      return BigDecimal.ZERO;
    }

    Coupon coupon = Coupon.fromCode(couponCode).orElse(null);
    if (coupon == null) {
      return BigDecimal.ZERO;
    }

    List<Coupon.OrderItem> couponItems = items.stream()
      .map(item -> new Coupon.OrderItem(item.sku(), item.quantity(), item.price()))
      .toList();

    Coupon.OrderContext ctx = new Coupon.OrderContext(
      couponItems,
      isFirstPurchase,
      shippingMethod,
      shippingCost
    );

    BigDecimal discount = coupon.calculateDiscount(productsSubtotal, ctx)
      .orElse(BigDecimal.ZERO);

    BigDecimal maxDiscount = productsSubtotal.multiply(new BigDecimal("0.30"));

    if (discount.compareTo(maxDiscount) > 0) {
      discount = maxDiscount;
    }

    return RoundingUtil.roundHalfEven(discount);
  }

  private BigDecimal calculateShippingCost(
    BigDecimal shippingCost,
    ClubTier clubTier,
    ShippingMethod shippingMethod,
    String couponCode
  ) {
    if (shippingMethod == ShippingMethod.STORE_PICKUP) {
      return BigDecimal.ZERO;
    }

    if (clubTier.isFreeShippingAllowed() && shippingMethod != ShippingMethod.COURIER) {
      return BigDecimal.ZERO;
    }

    if ("FREESHIPPING".equals(couponCode) && shippingMethod != ShippingMethod.STORE_PICKUP) {
      return BigDecimal.ZERO;
    }

    return shippingCost;
  }

  private BigDecimal calculateInsurance(BigDecimal productsSubtotal, Region region) {
    BigDecimal insurance = productsSubtotal.multiply(
      new BigDecimal(region.getInsuranceRate())
    );

    if (region.getInsuranceLimit() != null) {
      BigDecimal limit = region.getInsuranceLimit();
      if (insurance.compareTo(limit) > 0) {
        insurance = limit;
      }
    }

    return RoundingUtil.roundHalfEven(insurance);
  }

  private BigDecimal calculatePaymentAdjustment(
    BigDecimal subtotal,
    PaymentMethod paymentMethod,
    int installments,
    String couponCode
  ) {
    return switch (paymentMethod) {
      case PIX -> {
        if (couponCode != null && !couponCode.isEmpty()) {
          yield BigDecimal.ZERO;
        }
        yield subtotal.multiply(new BigDecimal("-0.05"));
      }
      case CREDIT_CARD -> {
        if (installments <= 3) {
          yield BigDecimal.ZERO;
        }
        BigDecimal monthlyRate = new BigDecimal("0.0199");
        BigDecimal numerator = subtotal.multiply(monthlyRate);
        BigDecimal denominator = new BigDecimal("1").subtract(
          new BigDecimal("1").add(monthlyRate).pow(-(installments),
            new java.math.MathContext(10))
        );
        BigDecimal installmentValue = numerator.divide(denominator, 10, java.math.RoundingMode.HALF_EVEN);
        BigDecimal totalWithInterest = installmentValue.multiply(new BigDecimal(installments));
        yield totalWithInterest.subtract(subtotal);
      }
      case BANK_SLIP -> new BigDecimal("3.49");
    };
  }

  private BigDecimal calculateInstallmentAmount(
    BigDecimal finalTotal,
    PaymentMethod paymentMethod,
    int installments
  ) {
    if (installments == 1) {
      return finalTotal;
    }

    return finalTotal.divide(
      new BigDecimal(installments),
      2,
      java.math.RoundingMode.HALF_EVEN
    );
  }

  private BigDecimal calculateCredit(BigDecimal productsSubtotal, ClubTier clubTier) {
    return RoundingUtil.roundHalfEven(
      productsSubtotal.multiply(new BigDecimal(clubTier.getCreditPercentage()))
    );
  }

  private boolean shouldGiveGift(BigDecimal productsSubtotal, ClubTier clubTier) {
    return clubTier == ClubTier.GOLD &&
           productsSubtotal.compareTo(new BigDecimal("500.00")) > 0;
  }

  public record SummaryItem(
    String sku,
    int quantity,
    BigDecimal price
  ) {}
}
