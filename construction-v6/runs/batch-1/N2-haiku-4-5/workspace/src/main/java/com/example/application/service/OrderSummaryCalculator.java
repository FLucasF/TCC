package com.example.application.service;

import com.example.api.dto.CheckoutRequestDto;
import com.example.domain.entity.OrderSummary;
import com.example.domain.entity.Product;
import com.example.domain.enums.*;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class OrderSummaryCalculator {

    private final ProductService productService;
    private final TransLogService transLogService;

    public OrderSummaryCalculator(ProductService productService, TransLogService transLogService) {
        this.productService = productService;
        this.transLogService = transLogService;
    }

    public OrderSummary calculate(CheckoutRequestDto request) {
        BigDecimal productsSubtotal = calculateProductsSubtotal(request);
        BigDecimal shippingCost = BigDecimal.ZERO;
        Integer deliveryDays = 0;

        ShippingMethod shippingMethod = ShippingMethod.valueOf(request.getShippingMethod());
        Region region = Region.valueOf(request.getRegion());
        ClubTier clubTier = ClubTier.valueOf(request.getClubTier());

        if (clubTier == ClubTier.GOLD && shippingMethod != ShippingMethod.COURIER && shippingMethod != ShippingMethod.STORE_PICKUP) {
            shippingCost = BigDecimal.ZERO;
            ShippingQuote quote = getShippingQuote(request, shippingMethod, BigDecimal.ZERO, region);
            if (quote != null) {
                deliveryDays = quote.days;
            }
        } else {
            ShippingQuote quote = getShippingQuote(request, shippingMethod, BigDecimal.ZERO, region);
            if (quote != null) {
                shippingCost = quote.cost;
                deliveryDays = quote.days;
            }
        }

        BigDecimal couponDiscount = calculateCouponDiscount(request, productsSubtotal, shippingCost, shippingMethod);
        BigDecimal insurance = calculateInsurance(productsSubtotal, region);
        BigDecimal subtotalBeforePayment = productsSubtotal
                .subtract(couponDiscount)
                .add(shippingCost)
                .add(insurance);

        BigDecimal paymentAdjustment = calculatePaymentAdjustment(request, subtotalBeforePayment);
        BigDecimal finalTotal = subtotalBeforePayment.add(paymentAdjustment);

        Integer installments = request.getInstallments() != null ? request.getInstallments() : 1;
        BigDecimal installmentAmount = calculateInstallmentAmount(finalTotal, request.getPaymentMethod(), installments);

        BigDecimal nextPurchaseCredit = calculateClubCredit(productsSubtotal, clubTier);
        Boolean gift = shouldGift(productsSubtotal, clubTier);

        return new OrderSummary(
                round(productsSubtotal),
                round(couponDiscount),
                round(shippingCost),
                deliveryDays,
                round(insurance),
                round(paymentAdjustment),
                round(finalTotal),
                installments,
                round(installmentAmount),
                round(nextPurchaseCredit),
                gift
        );
    }

    public BigDecimal calculateProductsSubtotal(CheckoutRequestDto request) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : request.getItems()) {
            Product product = productService.getProduct(item.getSku());
            BigDecimal itemTotal = product.getPrice().multiply(new BigDecimal(item.getQuantity()));
            subtotal = subtotal.add(itemTotal);
        }
        return subtotal;
    }

    private BigDecimal calculateCouponDiscount(CheckoutRequestDto request, BigDecimal productsSubtotal, BigDecimal shippingCost, ShippingMethod shippingMethod) {
        if (request.getCoupon() == null || request.getCoupon().isEmpty()) {
            return BigDecimal.ZERO;
        }

        CouponCode coupon = CouponCode.valueOf(request.getCoupon());
        BigDecimal discount = BigDecimal.ZERO;

        switch (coupon) {
            case WELCOME10:
                discount = productsSubtotal.multiply(new BigDecimal("0.10"));
                break;
            case MINUS50:
                if (productsSubtotal.compareTo(new BigDecimal("300")) >= 0) {
                    discount = new BigDecimal("50");
                }
                break;
            case FREESHIPPING:
                if (shippingMethod != ShippingMethod.STORE_PICKUP) {
                    discount = shippingCost;
                }
                break;
            case BUY3PAY2:
                discount = calculateBuy3Pay2Discount(request, productsSubtotal);
                break;
        }

        BigDecimal maxDiscount = productsSubtotal.multiply(new BigDecimal("0.30"));
        if (discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }

        return discount;
    }

    private BigDecimal calculateBuy3Pay2Discount(CheckoutRequestDto request, BigDecimal productsSubtotal) {
        BigDecimal discount = BigDecimal.ZERO;

        Map<String, Integer> skuQuantities = new HashMap<>();
        for (var item : request.getItems()) {
            skuQuantities.put(item.getSku(), item.getQuantity());
        }

        for (var entry : skuQuantities.entrySet()) {
            int quantity = entry.getValue();
            int freeUnits = quantity / 3;
            if (freeUnits > 0) {
                Product product = productService.getProduct(entry.getKey());
                discount = discount.add(product.getPrice().multiply(new BigDecimal(freeUnits)));
            }
        }

        return discount;
    }

    private ShippingQuote getShippingQuote(CheckoutRequestDto request, ShippingMethod method, BigDecimal currentCost, Region region) {
        ShippingQuote baseQuote = null;

        switch (method) {
            case ECONOMY:
            case EXPRESS:
                baseQuote = transLogService.getQuote(request.getZipCode(), calculateTotalWeight(request), method);
                break;
            case STORE_PICKUP:
                baseQuote = new ShippingQuote(BigDecimal.ZERO, 1);
                break;
            case COURIER:
                BigDecimal totalWeight = calculateTotalWeight(request);
                if (totalWeight.compareTo(new BigDecimal("5")) > 0) {
                    return null;
                }
                baseQuote = new ShippingQuote(new BigDecimal("18"), 0);
                break;
            default:
                return null;
        }

        if (baseQuote != null) {
            int adjustedDays = calculateDeliveryDays(method, region, baseQuote.days);
            return new ShippingQuote(baseQuote.cost, adjustedDays);
        }

        return baseQuote;
    }

    private Integer calculateDeliveryDays(ShippingMethod method, Region region, int baseDeliveryDays) {
        int days = baseDeliveryDays;

        if (method == ShippingMethod.ECONOMY || method == ShippingMethod.EXPRESS) {
            if (region == Region.NORTH) {
                days += 3;
            } else if (region == Region.NORTHEAST) {
                days += 2;
            }
        }

        return days;
    }

    private BigDecimal calculateTotalWeight(CheckoutRequestDto request) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (var item : request.getItems()) {
            Product product = productService.getProduct(item.getSku());
            BigDecimal itemWeight = product.getWeightKg().multiply(new BigDecimal(item.getQuantity()));
            totalWeight = totalWeight.add(itemWeight);
        }
        return totalWeight;
    }

    private BigDecimal calculateInsurance(BigDecimal productsSubtotal, Region region) {
        BigDecimal percentage;
        BigDecimal limit;

        switch (region) {
            case SOUTHEAST, SOUTH:
                percentage = new BigDecimal("0.01");
                limit = null;
                break;
            case MIDWEST:
                percentage = new BigDecimal("0.015");
                limit = null;
                break;
            case NORTH:
                percentage = new BigDecimal("0.025");
                limit = new BigDecimal("30");
                break;
            case NORTHEAST:
                percentage = new BigDecimal("0.02");
                limit = new BigDecimal("30");
                break;
            default:
                return BigDecimal.ZERO;
        }

        BigDecimal insurance = productsSubtotal.multiply(percentage);
        if (limit != null && insurance.compareTo(limit) > 0) {
            insurance = limit;
        }

        return insurance;
    }

    private BigDecimal calculatePaymentAdjustment(CheckoutRequestDto request, BigDecimal subtotal) {
        PaymentMethod method = PaymentMethod.valueOf(request.getPaymentMethod());
        Integer installments = request.getInstallments() != null ? request.getInstallments() : 1;

        switch (method) {
            case PIX:
                if (request.getCoupon() == null || request.getCoupon().isEmpty()) {
                    return subtotal.multiply(new BigDecimal("0.05")).negate();
                }
                return BigDecimal.ZERO;
            case CREDIT_CARD:
                if (installments <= 3) {
                    return BigDecimal.ZERO;
                }
                return calculateCreditCardInterest(subtotal, installments);
            case BANK_SLIP:
                return new BigDecimal("3.49");
            default:
                return BigDecimal.ZERO;
        }
    }

    private BigDecimal calculateCreditCardInterest(BigDecimal total, Integer installments) {
        BigDecimal monthlyRate = new BigDecimal("0.0199");
        BigDecimal installmentAmount = total
                .multiply(monthlyRate)
                .divide(
                        BigDecimal.ONE.subtract(
                                BigDecimal.ONE.add(monthlyRate).pow(-installments)
                        ),
                        10,
                        RoundingMode.HALF_EVEN
                );

        BigDecimal totalWithInterest = installmentAmount.multiply(new BigDecimal(installments));
        return totalWithInterest.subtract(total);
    }

    private BigDecimal calculateInstallmentAmount(BigDecimal finalTotal, String paymentMethod, Integer installments) {
        PaymentMethod method = PaymentMethod.valueOf(paymentMethod);

        if (method == PaymentMethod.CREDIT_CARD && installments > 3) {
            BigDecimal monthlyRate = new BigDecimal("0.0199");
            return finalTotal
                    .multiply(monthlyRate)
                    .divide(
                            BigDecimal.ONE.subtract(
                                    BigDecimal.ONE.add(monthlyRate).pow(-installments)
                            ),
                            2,
                            RoundingMode.HALF_EVEN
                    );
        }

        return finalTotal.divide(new BigDecimal(installments), 2, RoundingMode.HALF_EVEN);
    }

    private BigDecimal calculateClubCredit(BigDecimal productsSubtotal, ClubTier tier) {
        switch (tier) {
            case SILVER:
                return productsSubtotal.multiply(new BigDecimal("0.02"));
            case GOLD:
                return productsSubtotal.multiply(new BigDecimal("0.05"));
            default:
                return BigDecimal.ZERO;
        }
    }

    private Boolean shouldGift(BigDecimal productsSubtotal, ClubTier tier) {
        if (tier == ClubTier.GOLD && productsSubtotal.compareTo(new BigDecimal("500")) > 0) {
            return true;
        }
        return false;
    }

    private BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_EVEN);
    }

    public static class ShippingQuote {
        public BigDecimal cost;
        public Integer days;

        public ShippingQuote(BigDecimal cost, Integer days) {
            this.cost = cost;
            this.days = days;
        }
    }
}
