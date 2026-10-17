package com.loja.orderservice.service;

import com.loja.orderservice.client.TransLogClient;
import com.loja.orderservice.dto.OrderItemDTO;
import com.loja.orderservice.dto.OrderRequestDTO;
import com.loja.orderservice.entity.*;
import com.loja.orderservice.exception.OrderException;
import com.loja.orderservice.repository.ProductRepository;
import com.loja.orderservice.util.PricingUtil;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class PricingService {
    private final ProductRepository productRepository;
    private final TransLogClient transLogClient;

    public PricingService(ProductRepository productRepository, TransLogClient transLogClient) {
        this.productRepository = productRepository;
        this.transLogClient = transLogClient;
    }

    public OrderSummary calculateSummary(OrderRequestDTO request) {
        BigDecimal productsSubtotal = calculateProductsSubtotal(request.getItems());
        BigDecimal couponDiscount = calculateCouponDiscount(request.getCoupon(), productsSubtotal, request.getShippingMethod(), request);

        ShippingCostInfo shippingInfo = calculateShippingCost(request);
        BigDecimal shippingCost = shippingInfo.cost;
        Integer deliveryDays = shippingInfo.days;

        BigDecimal insurance = calculateInsurance(productsSubtotal, request.getRegion());

        BigDecimal subtotalBeforePayment = PricingUtil.add(
            PricingUtil.subtract(productsSubtotal, couponDiscount),
            PricingUtil.add(shippingCost, insurance)
        );

        validatePaymentMethodConstraints(request, subtotalBeforePayment);

        PaymentAdjustmentInfo adjustmentInfo = calculatePaymentAdjustment(
            request.getPaymentMethod(),
            subtotalBeforePayment,
            request.getInstallments(),
            request.getCoupon() != null && !request.getCoupon().isEmpty()
        );

        BigDecimal finalTotal = adjustmentInfo.finalTotal;
        Integer installments = adjustmentInfo.installments;
        BigDecimal installmentAmount = adjustmentInfo.installmentAmount;

        BigDecimal nextPurchaseCredit = calculateNextPurchaseCredit(request.getClubTier(), productsSubtotal);

        Boolean gift = shouldGiveGift(request.getClubTier(), productsSubtotal);

        return new OrderSummary(
            productsSubtotal,
            couponDiscount,
            shippingCost,
            deliveryDays,
            insurance,
            adjustmentInfo.adjustment,
            finalTotal,
            installments,
            installmentAmount,
            nextPurchaseCredit,
            gift
        );
    }

    private BigDecimal calculateProductsSubtotal(List<OrderItemDTO> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItemDTO item : items) {
            Optional<Product> product = productRepository.findById(item.getSku());
            if (product.isPresent()) {
                BigDecimal itemPrice = PricingUtil.multiply(product.get().getPrice(), new BigDecimal(item.getQuantity()));
                subtotal = PricingUtil.add(subtotal, itemPrice);
            }
        }
        return subtotal;
    }

    private BigDecimal calculateCouponDiscount(String coupon, BigDecimal subtotal, ShippingMethod shippingMethod, OrderRequestDTO request) {
        if (coupon == null || coupon.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = BigDecimal.ZERO;

        if ("WELCOME10".equals(coupon)) {
            discount = PricingUtil.percentage(subtotal, new BigDecimal("10"));
        } else if ("MINUS50".equals(coupon)) {
            if (subtotal.compareTo(new BigDecimal("300")) >= 0) {
                discount = new BigDecimal("50.00");
            }
        } else if ("FREESHIPPING".equals(coupon)) {
            ShippingCostInfo shippingInfo = calculateShippingCost(request);
            discount = shippingInfo.cost;
        } else if ("BUY3PAY2".equals(coupon)) {
            discount = calculateBuy3Pay2Discount(request.getItems());
        }

        BigDecimal maxDiscount = PricingUtil.percentage(subtotal, new BigDecimal("30"));
        if (discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }

        return discount;
    }

    private BigDecimal calculateBuy3Pay2Discount(List<OrderItemDTO> items) {
        BigDecimal discount = BigDecimal.ZERO;

        for (OrderItemDTO item : items) {
            Optional<Product> product = productRepository.findById(item.getSku());
            if (product.isPresent()) {
                int quantity = item.getQuantity();
                int freeItems = quantity / 3;
                if (freeItems > 0) {
                    BigDecimal itemDiscount = PricingUtil.multiply(product.get().getPrice(), new BigDecimal(freeItems));
                    discount = PricingUtil.add(discount, itemDiscount);
                }
            }
        }

        return discount;
    }

    private class ShippingCostInfo {
        BigDecimal cost;
        Integer days;

        ShippingCostInfo(BigDecimal cost, Integer days) {
            this.cost = cost;
            this.days = days;
        }
    }

    private ShippingCostInfo calculateShippingCost(OrderRequestDTO request) {
        ShippingMethod method = request.getShippingMethod();
        Region region = request.getRegion();
        String zipCode = request.getZipCode();
        BigDecimal weight = calculateWeight(request.getItems());

        if (method == ShippingMethod.STORE_PICKUP) {
            return new ShippingCostInfo(BigDecimal.ZERO, 1);
        }

        if (method == ShippingMethod.COURIER) {
            if (weight.compareTo(new BigDecimal("5")) > 0) {
                throw new OrderException("SHIPPING_METHOD_UNAVAILABLE", "COURIER max weight is 5kg", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            return new ShippingCostInfo(new BigDecimal("18.00"), 0);
        }

        try {
            String service = method == ShippingMethod.ECONOMY ? "STD" : "EXP";
            TransLogClient.TransLogQuote quote = transLogClient.getQuote(zipCode, weight.multiply(new BigDecimal("1000")).intValue(), service);

            BigDecimal cost = PricingUtil.round(quote.getPrice());
            int days = PricingUtil.ceilDays(quote.etaHours);

            if (region == Region.NORTH && (method == ShippingMethod.ECONOMY || method == ShippingMethod.EXPRESS)) {
                days += 3;
            } else if (region == Region.NORTHEAST && (method == ShippingMethod.ECONOMY || method == ShippingMethod.EXPRESS)) {
                days += 2;
            }

            return new ShippingCostInfo(cost, days);
        } catch (TransLogClient.TransLogException e) {
            if ("ZIP_NOT_SERVED".equals(e.getMessage())) {
                throw new OrderException("SHIPPING_METHOD_UNAVAILABLE", "TransLog does not serve this ZIP", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            throw new OrderException("CARRIER_UNAVAILABLE", "TransLog is unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private BigDecimal calculateWeight(List<OrderItemDTO> items) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        for (OrderItemDTO item : items) {
            Optional<Product> product = productRepository.findById(item.getSku());
            if (product.isPresent()) {
                BigDecimal itemWeight = product.get().getWeightKg().multiply(new BigDecimal(item.getQuantity()));
                totalWeight = totalWeight.add(itemWeight);
            }
        }
        return totalWeight;
    }

    private BigDecimal calculateInsurance(BigDecimal subtotal, Region region) {
        BigDecimal percentage;
        BigDecimal limit = null;

        switch (region) {
            case SOUTHEAST, SOUTH -> percentage = new BigDecimal("1");
            case MIDWEST -> percentage = new BigDecimal("1.5");
            case NORTH -> {
                percentage = new BigDecimal("2.5");
                limit = new BigDecimal("30");
            }
            case NORTHEAST -> {
                percentage = new BigDecimal("2");
                limit = new BigDecimal("30");
            }
            default -> percentage = BigDecimal.ZERO;
        }

        BigDecimal insurance = PricingUtil.percentage(subtotal, percentage);

        if (limit != null && insurance.compareTo(limit) > 0) {
            insurance = limit;
        }

        return insurance;
    }

    private void validatePaymentMethodConstraints(OrderRequestDTO request, BigDecimal total) {
        if (request.getPaymentMethod() == PaymentMethod.BANK_SLIP) {
            if (total.compareTo(new BigDecimal("1000")) > 0) {
                throw new OrderException("PAYMENT_METHOD_UNAVAILABLE", "BANK_SLIP not available for orders over R$ 1.000,00", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private class PaymentAdjustmentInfo {
        BigDecimal finalTotal;
        BigDecimal adjustment;
        Integer installments;
        BigDecimal installmentAmount;

        PaymentAdjustmentInfo(BigDecimal finalTotal, BigDecimal adjustment, Integer installments, BigDecimal installmentAmount) {
            this.finalTotal = finalTotal;
            this.adjustment = adjustment;
            this.installments = installments;
            this.installmentAmount = installmentAmount;
        }
    }

    private PaymentAdjustmentInfo calculatePaymentAdjustment(PaymentMethod method, BigDecimal subtotal, Integer installments, boolean hasCoupon) {
        int inst = installments != null ? installments : 1;
        BigDecimal adjustment = BigDecimal.ZERO;
        BigDecimal finalTotal = subtotal;

        if (method == PaymentMethod.PIX && !hasCoupon) {
            BigDecimal discount = PricingUtil.percentage(subtotal, new BigDecimal("5"));
            adjustment = discount.negate();
            finalTotal = PricingUtil.subtract(subtotal, discount);
        } else if (method == PaymentMethod.BANK_SLIP) {
            adjustment = new BigDecimal("3.49");
            finalTotal = PricingUtil.add(subtotal, adjustment);
        } else if (method == PaymentMethod.CREDIT_CARD) {
            if (inst <= 3) {
                finalTotal = subtotal;
                adjustment = BigDecimal.ZERO;
            } else {
                BigDecimal monthlyRate = new BigDecimal("0.0199");
                BigDecimal multiplier = calculateInstallmentMultiplier(monthlyRate, inst);
                BigDecimal installmentValue = PricingUtil.multiply(subtotal, multiplier);
                finalTotal = PricingUtil.multiply(installmentValue, new BigDecimal(inst));
                adjustment = PricingUtil.subtract(finalTotal, subtotal);
            }
        }

        BigDecimal installmentAmount = PricingUtil.divide(finalTotal, new BigDecimal(inst));

        return new PaymentAdjustmentInfo(finalTotal, adjustment, inst, installmentAmount);
    }

    private BigDecimal calculateInstallmentMultiplier(BigDecimal monthlyRate, Integer installments) {
        BigDecimal numerator = monthlyRate;
        BigDecimal denominator = BigDecimal.ONE;
        BigDecimal monthlyRatePlus1 = monthlyRate.add(BigDecimal.ONE);

        BigDecimal power = monthlyRatePlus1.pow(installments);
        denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(power, 10, java.math.RoundingMode.HALF_EVEN));

        return numerator.divide(denominator, 10, java.math.RoundingMode.HALF_EVEN);
    }

    private BigDecimal calculateNextPurchaseCredit(ClubTier tier, BigDecimal subtotal) {
        if (tier == ClubTier.SILVER) {
            return PricingUtil.percentage(subtotal, new BigDecimal("2"));
        } else if (tier == ClubTier.GOLD) {
            return PricingUtil.percentage(subtotal, new BigDecimal("5"));
        }
        return BigDecimal.ZERO;
    }

    private Boolean shouldGiveGift(ClubTier tier, BigDecimal subtotal) {
        if (tier == ClubTier.GOLD && subtotal.compareTo(new BigDecimal("500")) > 0) {
            return true;
        }
        return false;
    }
}
