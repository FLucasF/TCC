package com.loja.service;

import com.loja.domain.Order;
import com.loja.domain.Product;
import com.loja.dto.CheckoutRequest;
import com.loja.dto.SummaryResponse;
import com.loja.exception.OrderException;
import com.loja.repository.OrderRepository;
import com.loja.repository.ProductRepository;
import com.loja.strategy.club.ClubBenefits;
import com.loja.strategy.club.ClubTierStrategy;
import com.loja.strategy.coupon.CouponStrategy;
import com.loja.strategy.payment.PaymentResult;
import com.loja.strategy.payment.PaymentStrategy;
import com.loja.strategy.shipping.ShippingResult;
import com.loja.strategy.shipping.ShippingStrategy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CheckoutService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final Map<String, CouponStrategy> coupons;
    private final Map<String, ShippingStrategy> shippingMethods;
    private final Map<String, PaymentStrategy> paymentMethods;
    private final Map<String, ClubTierStrategy> clubTiers;

    public CheckoutService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            List<CouponStrategy> couponList,
            List<ShippingStrategy> shippingList,
            List<PaymentStrategy> paymentList,
            List<ClubTierStrategy> clubList) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.coupons = couponList.stream().collect(Collectors.toMap(CouponStrategy::getCode, c -> c));
        this.shippingMethods = shippingList.stream().collect(Collectors.toMap(ShippingStrategy::getCode, s -> s));
        this.paymentMethods = paymentList.stream().collect(Collectors.toMap(PaymentStrategy::getCode, p -> p));
        this.clubTiers = clubList.stream().collect(Collectors.toMap(ClubTierStrategy::getCode, t -> t));
    }

    @Transactional(readOnly = true)
    public SummaryResponse calculateSummary(CheckoutRequest request) {
        validateRequest(request);

        Map<String, Product> productMap = loadProducts(request);

        BigDecimal productsSubtotal = calculateProductsSubtotal(request, productMap);
        BigDecimal totalWeightKg = calculateTotalWeight(request, productMap);

        ShippingResult shippingResult = calculateShipping(request, totalWeightKg);

        ClubTierStrategy clubTier = getClubTier(request.getClubTier());
        ClubBenefits clubBenefits = clubTier.getBenefits(request, productsSubtotal);

        BigDecimal shippingCost = clubBenefits.isFreeShipping() ? BigDecimal.ZERO : shippingResult.getCost();
        int deliveryDays = shippingResult.getDeliveryDays();

        BigDecimal couponDiscount = calculateCouponDiscount(request, productMap, productsSubtotal, shippingCost);

        BigDecimal insurance = calculateInsurance(request, productsSubtotal);

        BigDecimal orderTotal = productsSubtotal
            .subtract(couponDiscount)
            .add(shippingCost)
            .add(insurance);

        PaymentResult paymentResult = calculatePayment(request, orderTotal);

        BigDecimal credit = MoneyRounding.round(
            productsSubtotal.multiply(clubBenefits.getCreditPercentage())
        );

        boolean gift = clubBenefits.getGiftThreshold() != null &&
                      productsSubtotal.compareTo(clubBenefits.getGiftThreshold()) > 0;

        SummaryResponse summary = new SummaryResponse();
        summary.setProductsSubtotal(productsSubtotal);
        summary.setCouponDiscount(couponDiscount);
        summary.setShippingCost(shippingCost);
        summary.setDeliveryDays(deliveryDays);
        summary.setInsurance(insurance);
        summary.setPaymentAdjustment(paymentResult.getAdjustment());
        summary.setFinalTotal(paymentResult.getFinalTotal());
        summary.setInstallments(paymentResult.getInstallments());
        summary.setInstallmentAmount(paymentResult.getInstallmentAmount());
        summary.setNextPurchaseCredit(credit);
        summary.setGift(gift);

        return summary;
    }

    private void validateRequest(CheckoutRequest request) {
        if (request.getCustomer() == null || request.getCustomer().getCpf() == null ||
            request.getCustomer().getEmail() == null) {
            throw new OrderException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Map<String, Long> skuCount = request.getItems().stream()
            .collect(Collectors.groupingBy(item -> item.getSku(), Collectors.counting()));

        for (var item : request.getItems()) {
            if (item.getSku() == null || item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            if (skuCount.get(item.getSku()) > 1) {
                throw new OrderException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private Map<String, Product> loadProducts(CheckoutRequest request) {
        List<String> skus = request.getItems().stream()
            .map(item -> item.getSku())
            .collect(Collectors.toList());

        List<Product> products = productRepository.findAllById(skus);

        if (products.size() != skus.size()) {
            throw new OrderException("UNKNOWN_PRODUCT", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Map<String, Product> productMap = new HashMap<>();
        for (Product product : products) {
            productMap.put(product.getSku(), product);

            if (product.getDiscontinued()) {
                throw new OrderException("PRODUCT_DISCONTINUED", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            int requestedQty = request.getItems().stream()
                .filter(item -> item.getSku().equals(product.getSku()))
                .findFirst()
                .get()
                .getQuantity();

            if (product.getStock() < requestedQty) {
                throw new OrderException("INSUFFICIENT_STOCK", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        return productMap;
    }

    private BigDecimal calculateProductsSubtotal(CheckoutRequest request, Map<String, Product> products) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : request.getItems()) {
            Product product = products.get(item.getSku());
            subtotal = subtotal.add(product.getPrice().multiply(new BigDecimal(item.getQuantity())));
        }
        return MoneyRounding.round(subtotal);
    }

    private BigDecimal calculateTotalWeight(CheckoutRequest request, Map<String, Product> products) {
        BigDecimal weight = BigDecimal.ZERO;
        for (var item : request.getItems()) {
            Product product = products.get(item.getSku());
            weight = weight.add(product.getWeightKg().multiply(new BigDecimal(item.getQuantity())));
        }
        return weight;
    }

    private ShippingResult calculateShipping(CheckoutRequest request, BigDecimal totalWeightKg) {
        if (request.getClubTier() == null || !clubTiers.containsKey(request.getClubTier())) {
            throw new OrderException("INVALID_CLUB_TIER", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getRegion() == null) {
            throw new OrderException("INVALID_REGION", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (!List.of("SOUTHEAST", "SOUTH", "MIDWEST", "NORTH", "NORTHEAST").contains(request.getRegion())) {
            throw new OrderException("INVALID_REGION", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getZipCode() == null || !request.getZipCode().matches("\\d{8}")) {
            throw new OrderException("INVALID_ZIP_CODE", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (request.getShippingMethod() == null || !shippingMethods.containsKey(request.getShippingMethod())) {
            throw new OrderException("INVALID_SHIPPING_METHOD", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        ShippingStrategy shipping = shippingMethods.get(request.getShippingMethod());

        if (!shipping.isAvailable(request, totalWeightKg)) {
            if (request.getShippingMethod().equals("ECONOMY") || request.getShippingMethod().equals("EXPRESS")) {
                throw new OrderException("CARRIER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
            }
            throw new OrderException("SHIPPING_METHOD_UNAVAILABLE", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        try {
            return shipping.calculate(request, totalWeightKg);
        } catch (Exception e) {
            throw new OrderException("CARRIER_UNAVAILABLE", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private BigDecimal calculateCouponDiscount(CheckoutRequest request, Map<String, Product> products,
                                                BigDecimal productsSubtotal, BigDecimal shippingCost) {
        if (request.getCoupon() == null) {
            return BigDecimal.ZERO;
        }

        if (!coupons.containsKey(request.getCoupon())) {
            throw new OrderException("INVALID_COUPON", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        CouponStrategy coupon = coupons.get(request.getCoupon());

        List<String> customerStatuses = orderRepository
            .findByCustomerCpfAndStatusIn(request.getCustomer().getCpf(),
                                         List.of("PAID", "SHIPPED", "DELIVERED"))
            .stream()
            .map(Order::getStatus)
            .collect(Collectors.toList());

        if (!coupon.isApplicable(request, customerStatuses)) {
            throw new OrderException("COUPON_NOT_APPLICABLE", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        BigDecimal discount = coupon.calculateDiscount(request, products, productsSubtotal, shippingCost);

        if (request.getCoupon().equals("MINUS50")) {
            if (productsSubtotal.compareTo(new BigDecimal("300.00")) < 0) {
                throw new OrderException("COUPON_NOT_APPLICABLE", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        BigDecimal maxDiscount = MoneyRounding.round(productsSubtotal.multiply(new BigDecimal("0.30")));
        if (discount.compareTo(maxDiscount) > 0) {
            discount = maxDiscount;
        }

        return discount;
    }

    private BigDecimal calculateInsurance(CheckoutRequest request, BigDecimal productsSubtotal) {
        BigDecimal percentage;
        BigDecimal limit = null;

        switch (request.getRegion()) {
            case "SOUTHEAST", "SOUTH" -> percentage = new BigDecimal("0.01");
            case "MIDWEST" -> percentage = new BigDecimal("0.015");
            case "NORTH" -> {
                percentage = new BigDecimal("0.025");
                limit = new BigDecimal("30.00");
            }
            case "NORTHEAST" -> {
                percentage = new BigDecimal("0.02");
                limit = new BigDecimal("30.00");
            }
            default -> throw new OrderException("INVALID_REGION", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        BigDecimal insurance = MoneyRounding.round(productsSubtotal.multiply(percentage));

        if (limit != null && insurance.compareTo(limit) > 0) {
            insurance = limit;
        }

        return insurance;
    }

    private PaymentResult calculatePayment(CheckoutRequest request, BigDecimal orderTotal) {
        if (request.getPaymentMethod() == null || !paymentMethods.containsKey(request.getPaymentMethod())) {
            throw new OrderException("INVALID_PAYMENT_METHOD", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        PaymentStrategy payment = paymentMethods.get(request.getPaymentMethod());

        int installments = request.getInstallments() != null ? request.getInstallments() : 1;

        if (!payment.isValidInstallments(installments)) {
            throw new OrderException("INVALID_INSTALLMENTS", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (!payment.isAvailable(request, orderTotal)) {
            throw new OrderException("PAYMENT_METHOD_UNAVAILABLE", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        boolean hasCoupon = request.getCoupon() != null;

        return payment.calculate(request, orderTotal, hasCoupon);
    }

    private ClubTierStrategy getClubTier(String tierCode) {
        if (tierCode == null || !clubTiers.containsKey(tierCode)) {
            throw new OrderException("INVALID_CLUB_TIER", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        return clubTiers.get(tierCode);
    }
}
