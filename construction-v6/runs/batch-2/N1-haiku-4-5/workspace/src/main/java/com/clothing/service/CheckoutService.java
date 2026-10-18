package com.clothing.service;

import com.clothing.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;

public class CheckoutService {
    private static final Logger logger = LoggerFactory.getLogger(CheckoutService.class);
    private final ProductRepository productRepo;
    private final CustomerRepository customerRepo;
    private final OrderRepository orderRepo;

    public CheckoutService(ProductRepository productRepo, CustomerRepository customerRepo, OrderRepository orderRepo) {
        this.productRepo = productRepo;
        this.customerRepo = customerRepo;
        this.orderRepo = orderRepo;
    }

    public Summary calculateSummary(CheckoutRequest req) {
        BigDecimal subtotal = calculateSubtotal(req);

        BigDecimal couponDiscount = calculateCouponDiscount(req, subtotal);

        BigDecimal shippingCost = calculateShippingCost(req);
        int deliveryDays = calculateDeliveryDays(req);

        BigDecimal insurance = calculateInsurance(req, subtotal);

        BigDecimal beforePayment = Money.add(Money.subtract(subtotal, couponDiscount), Money.add(shippingCost, insurance));

        BigDecimal paymentAdjustment = calculatePaymentAdjustment(req, beforePayment);

        BigDecimal finalTotal = Money.add(beforePayment, paymentAdjustment);

        BigDecimal nextPurchaseCredit = calculateNextPurchaseCredit(req, subtotal);

        boolean gift = shouldGetGift(req, subtotal);

        int installments = req.getInstallmentsOrDefault();
        BigDecimal installmentAmount = Money.divide(finalTotal, new BigDecimal(installments));

        return new Summary(
            subtotal,
            couponDiscount,
            shippingCost,
            deliveryDays,
            insurance,
            paymentAdjustment,
            finalTotal,
            installments,
            installmentAmount,
            nextPurchaseCredit,
            gift
        );
    }

    private BigDecimal calculateSubtotal(CheckoutRequest req) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderItem item : req.items()) {
            Product product = productRepo.findBySku(item.sku());
            if (product != null) {
                subtotal = Money.add(subtotal, Money.multiply(product.price(), new BigDecimal(item.quantity())));
            }
        }
        return subtotal;
    }

    private BigDecimal calculateCouponDiscount(CheckoutRequest req, BigDecimal subtotal) {
        if (req.coupon() == null || req.coupon().isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal discount = switch (req.coupon()) {
            case "WELCOME10" -> Money.multiply(subtotal, new BigDecimal("0.10"));
            case "MINUS50" -> new BigDecimal("50.00");
            case "FREESHIPPING" -> {
                if (req.shippingMethod() == ShippingMethod.STORE_PICKUP) {
                    yield BigDecimal.ZERO;
                } else {
                    yield calculateShippingCostRaw(req);
                }
            }
            case "BUY3PAY2" -> calculateBuy3Pay2Discount(req);
            default -> BigDecimal.ZERO;
        };

        BigDecimal max30Percent = Money.multiply(subtotal, new BigDecimal("0.30"));
        return Money.min(discount, max30Percent);
    }

    private BigDecimal calculateBuy3Pay2Discount(CheckoutRequest req) {
        BigDecimal totalDiscount = BigDecimal.ZERO;
        for (OrderItem item : req.items()) {
            if (item.quantity() >= 3) {
                Product product = productRepo.findBySku(item.sku());
                if (product != null) {
                    int freeUnits = item.quantity() / 3;
                    totalDiscount = Money.add(totalDiscount, Money.multiply(product.price(), new BigDecimal(freeUnits)));
                }
            }
        }
        return totalDiscount;
    }

    private BigDecimal calculateShippingCost(CheckoutRequest req) {
        if (req.clubTier() == ClubTier.GOLD && req.shippingMethod() != ShippingMethod.COURIER) {
            return BigDecimal.ZERO;
        }
        return calculateShippingCostRaw(req);
    }

    private BigDecimal calculateShippingCostRaw(CheckoutRequest req) {
        return switch (req.shippingMethod()) {
            case STORE_PICKUP -> BigDecimal.ZERO;
            case COURIER -> new BigDecimal("18.00");
            case ECONOMY, EXPRESS -> {
                try {
                    double totalWeightKg = calculateTotalWeight(req);
                    String service = req.shippingMethod() == ShippingMethod.ECONOMY ? "STD" : "EXP";
                    var quote = TransLogClient.quote(req.zipCode(), totalWeightKg, service);
                    if (quote != null) {
                        yield quote.price();
                    }
                    yield BigDecimal.ZERO;
                } catch (TransLogClient.CarrierUnavailableException e) {
                    yield BigDecimal.ZERO;
                }
            }
        };
    }

    private int calculateDeliveryDays(CheckoutRequest req) {
        int baseDays = switch (req.shippingMethod()) {
            case STORE_PICKUP -> 1;
            case COURIER -> 0;
            case ECONOMY, EXPRESS -> {
                try {
                    double totalWeightKg = calculateTotalWeight(req);
                    String service = req.shippingMethod() == ShippingMethod.ECONOMY ? "STD" : "EXP";
                    var quote = TransLogClient.quote(req.zipCode(), totalWeightKg, service);
                    if (quote != null) {
                        yield quote.deliveryDays();
                    }
                    yield 0;
                } catch (TransLogClient.CarrierUnavailableException e) {
                    yield 0;
                }
            }
        };
        return baseDays + req.region().getExtraDeliveryDays();
    }

    private double calculateTotalWeight(CheckoutRequest req) {
        double total = 0;
        for (OrderItem item : req.items()) {
            Product product = productRepo.findBySku(item.sku());
            if (product != null) {
                total += product.weightKg().doubleValue() * item.quantity();
            }
        }
        return total;
    }

    private BigDecimal calculateInsurance(CheckoutRequest req, BigDecimal subtotal) {
        BigDecimal percentage = req.region().insuranceRate;
        BigDecimal insurance = Money.multiply(subtotal, percentage);

        if (req.region().insuranceLimit != null) {
            insurance = Money.min(insurance, req.region().insuranceLimit);
        }

        return insurance;
    }

    private BigDecimal calculatePaymentAdjustment(CheckoutRequest req, BigDecimal beforePayment) {
        return switch (req.paymentMethod()) {
            case PIX -> {
                if (req.coupon() == null || req.coupon().isEmpty()) {
                    yield Money.multiply(beforePayment, new BigDecimal("-0.05"));
                } else {
                    yield BigDecimal.ZERO;
                }
            }
            case BANK_SLIP -> new BigDecimal("3.49");
            case CREDIT_CARD -> {
                int installments = req.getInstallmentsOrDefault();
                if (installments <= 3) {
                    yield BigDecimal.ZERO;
                }
                BigDecimal monthlyRate = new BigDecimal("0.0199");
                BigDecimal rateComplement = monthlyRate.add(BigDecimal.ONE);
                BigDecimal ratePower = rateComplement.pow(installments, new java.math.MathContext(10));
                BigDecimal divisor = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(ratePower, 10, java.math.RoundingMode.HALF_EVEN));
                BigDecimal singlePayment = Money.divide(Money.multiply(beforePayment, monthlyRate), divisor);
                BigDecimal totalWithInterest = Money.multiply(singlePayment, new BigDecimal(installments));
                yield Money.subtract(totalWithInterest, beforePayment);
            }
        };
    }

    private BigDecimal calculateNextPurchaseCredit(CheckoutRequest req, BigDecimal subtotal) {
        return switch (req.clubTier()) {
            case BRONZE -> BigDecimal.ZERO;
            case SILVER -> Money.multiply(subtotal, new BigDecimal("0.02"));
            case GOLD -> Money.multiply(subtotal, new BigDecimal("0.05"));
        };
    }

    private boolean shouldGetGift(CheckoutRequest req, BigDecimal subtotal) {
        return req.clubTier() == ClubTier.GOLD && subtotal.compareTo(new BigDecimal("500.00")) > 0;
    }

    public Order registerOrder(CheckoutRequest req) {
        Summary summary = calculateSummary(req);
        customerRepo.findOrCreate(req.customer().cpf(), req.customer().email());
        return orderRepo.createOrder(req, summary);
    }

    public void processPayment(long orderId, String transactionId, Instant paidAt, Order order, String cpf) throws Exception {
        Instant createdAt = orderRepo.getOrderCreatedAt(orderId);
        if (createdAt != null) {
            long diffSeconds = java.time.temporal.ChronoUnit.SECONDS.between(createdAt, paidAt);
            if (diffSeconds > 48 * 3600) {
                orderRepo.updateOrderStatus(orderId, OrderStatus.CANCELLED);
                customerRepo.createNotification(cpf, orderId, NotificationType.ORDER_CANCELLED);
                throw new Exception("ORDER_EXPIRED");
            }
        }

        orderRepo.recordPayment(orderId, transactionId, paidAt);
        orderRepo.updateOrderStatusAndTimestamp(orderId, OrderStatus.PAID, "paid_at");

        for (OrderItem item : order.items()) {
            orderRepo.decrementStock(item.sku(), item.quantity());
        }

        BigDecimal credit = order.summary().nextPurchaseCredit();
        if (credit.compareTo(BigDecimal.ZERO) > 0) {
            customerRepo.updateCreditBalance(cpf, credit);
        }

        customerRepo.createNotification(cpf, orderId, NotificationType.PAYMENT_APPROVED);
    }
}
