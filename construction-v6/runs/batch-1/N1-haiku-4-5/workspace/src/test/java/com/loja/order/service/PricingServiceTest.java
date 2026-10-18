package com.loja.order.service;

import com.loja.order.domain.*;
import com.loja.order.model.OrderSummary;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PricingServiceTest {
  private final PricingService pricingService = new PricingService();

  @Test
  void shouldCalculateSummaryWithBasicOrder() {
    List<PricingService.SummaryItem> items = List.of(
      new PricingService.SummaryItem("CAM-001", 2, new BigDecimal("79.90"))
    );

    OrderSummary summary = pricingService.calculateSummary(
      items,
      null,
      ShippingMethod.STORE_PICKUP,
      BigDecimal.ZERO,
      1,
      Region.SOUTHEAST,
      PaymentMethod.PIX,
      1,
      ClubTier.BRONZE,
      true
    );

    assertEquals(new BigDecimal("159.80"), summary.getProductsSubtotal());
    assertEquals(new BigDecimal("0.00"), summary.getCouponDiscount());
    assertEquals(new BigDecimal("0.00"), summary.getShippingCost());
    assertEquals(1, summary.getDeliveryDays());
    assertNotNull(summary.getFinalTotal());
    assertEquals(1, summary.getInstallments());
  }

  @Test
  void shouldApplyWelcomeCoupon() {
    List<PricingService.SummaryItem> items = List.of(
      new PricingService.SummaryItem("CAM-001", 1, new BigDecimal("79.90"))
    );

    OrderSummary summary = pricingService.calculateSummary(
      items,
      "WELCOME10",
      ShippingMethod.STORE_PICKUP,
      BigDecimal.ZERO,
      1,
      Region.SOUTHEAST,
      PaymentMethod.PIX,
      1,
      ClubTier.BRONZE,
      true
    );

    assertTrue(summary.getCouponDiscount().compareTo(BigDecimal.ZERO) > 0);
  }

  @Test
  void shouldNotApplyWelcomeCouponForRepeatingCustomer() {
    List<PricingService.SummaryItem> items = List.of(
      new PricingService.SummaryItem("CAM-001", 1, new BigDecimal("79.90"))
    );

    OrderSummary summary = pricingService.calculateSummary(
      items,
      "WELCOME10",
      ShippingMethod.STORE_PICKUP,
      BigDecimal.ZERO,
      1,
      Region.SOUTHEAST,
      PaymentMethod.PIX,
      1,
      ClubTier.BRONZE,
      false
    );

    assertEquals(new BigDecimal("0.00"), summary.getCouponDiscount());
  }

  @Test
  void shouldApplyGoldClubBenefit() {
    List<PricingService.SummaryItem> items = List.of(
      new PricingService.SummaryItem("JAQ-001", 3, new BigDecimal("389.90")),
      new PricingService.SummaryItem("TEN-001", 1, new BigDecimal("249.90"))
    );

    OrderSummary summary = pricingService.calculateSummary(
      items,
      null,
      ShippingMethod.EXPRESS,
      new BigDecimal("50.00"),
      3,
      Region.SOUTHEAST,
      PaymentMethod.PIX,
      1,
      ClubTier.GOLD,
      false
    );

    assertEquals(new BigDecimal("0.00"), summary.getShippingCost());
    assertTrue(summary.getNextPurchaseCredit().compareTo(BigDecimal.ZERO) > 0);
    assertTrue(summary.isGift());
  }
}
