package com.loja.service;

import com.loja.domain.*;
import com.loja.dto.OrderRequest;
import com.loja.exception.ErrorCode;
import com.loja.integration.TransLogUnavailableException;
import com.loja.repository.OrderRepository;
import com.loja.repository.ProductRepository;
import com.loja.service.club.ClubTierStrategy;
import com.loja.service.coupon.CouponStrategy;
import com.loja.service.payment.PaymentStrategy;
import com.loja.service.shipping.ShippingStrategy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CheckoutService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final InsuranceService insuranceService;
    private final Map<CouponCode, CouponStrategy> couponStrategies;
    private final Map<ShippingMethod, ShippingStrategy> shippingStrategies;
    private final Map<PaymentMethod, PaymentStrategy> paymentStrategies;
    private final Map<ClubTier, ClubTierStrategy> clubStrategies;

    public CheckoutService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            InsuranceService insuranceService,
            List<CouponStrategy> coupons,
            List<ShippingStrategy> shippings,
            List<PaymentStrategy> payments,
            List<ClubTierStrategy> clubs) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.insuranceService = insuranceService;
        this.couponStrategies = coupons.stream()
            .collect(Collectors.toMap(CouponStrategy::getCode, c -> c));
        this.shippingStrategies = shippings.stream()
            .collect(Collectors.toMap(ShippingStrategy::getMethod, s -> s));
        this.paymentStrategies = payments.stream()
            .collect(Collectors.toMap(PaymentStrategy::getMethod, p -> p));
        this.clubStrategies = clubs.stream()
            .collect(Collectors.toMap(ClubTierStrategy::getTier, c -> c));
    }

    @Transactional(readOnly = true)
    public OrderSummary calculateSummary(OrderRequest request) {
        validateRequest(request);

        Map<String, Product> products = loadProducts(request);

        Money productsSubtotal = calculateProductsSubtotal(request.items(), products);

        Weight totalWeight = calculateTotalWeight(request.items(), products);

        ClubTierStrategy clubStrategy = getClubStrategy(request.clubTier());

        ShippingStrategy.ShippingResult shipping = calculateShipping(
            request, totalWeight, clubStrategy
        );

        Money couponDiscount = calculateCouponDiscount(
            request, productsSubtotal, shipping.cost(), products
        );

        Money insurance = insuranceService.calculateInsurance(productsSubtotal, request.region());

        Money orderTotal = productsSubtotal
            .subtract(couponDiscount)
            .add(shipping.cost())
            .add(insurance);

        PaymentStrategy.PaymentResult payment = calculatePayment(request, orderTotal);

        Money nextPurchaseCredit = clubStrategy.calculateCredit(productsSubtotal);
        boolean gift = clubStrategy.hasGift(productsSubtotal);

        return new OrderSummary(
            productsSubtotal,
            couponDiscount,
            shipping.cost(),
            shipping.days(),
            insurance,
            payment.adjustment(),
            payment.finalTotal(),
            request.getInstallments(),
            payment.installmentAmount(),
            nextPurchaseCredit,
            gift
        );
    }

    private void validateRequest(OrderRequest request) {
        if (request.customer() == null || request.customer().cpf() == null || request.customer().email() == null) {
            throw ErrorCode.INVALID_ORDER.asException();
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw ErrorCode.INVALID_ORDER.asException();
        }

        Set<String> seenSkus = new HashSet<>();
        for (var item : request.items()) {
            if (item.sku() == null || item.quantity() == null || item.quantity() <= 0) {
                throw ErrorCode.INVALID_ORDER.asException();
            }
            if (!seenSkus.add(item.sku())) {
                throw ErrorCode.INVALID_ORDER.asException();
            }
        }
    }

    private Map<String, Product> loadProducts(OrderRequest request) {
        List<String> skus = request.items().stream()
            .map(OrderRequest.ItemInfo::sku)
            .toList();

        List<Product> products = productRepository.findAllById(skus);

        if (products.size() != skus.size()) {
            throw ErrorCode.UNKNOWN_PRODUCT.asException();
        }

        Map<String, Product> productMap = products.stream()
            .collect(Collectors.toMap(Product::getSku, p -> p));

        for (Product product : products) {
            if (product.isDiscontinued()) {
                throw ErrorCode.PRODUCT_DISCONTINUED.asException();
            }
        }

        for (var item : request.items()) {
            Product product = productMap.get(item.sku());
            if (!product.hasStock(item.quantity())) {
                throw ErrorCode.INSUFFICIENT_STOCK.asException();
            }
        }

        return productMap;
    }

    private Money calculateProductsSubtotal(List<OrderRequest.ItemInfo> items, Map<String, Product> products) {
        Money total = Money.zero();
        for (var item : items) {
            Product product = products.get(item.sku());
            Money itemTotal = product.getPrice().multiply(item.quantity());
            total = total.add(itemTotal);
        }
        return total;
    }

    private Weight calculateTotalWeight(List<OrderRequest.ItemInfo> items, Map<String, Product> products) {
        Weight total = new Weight(0);
        for (var item : items) {
            Product product = products.get(item.sku());
            Weight itemWeight = product.getWeight().multiply(item.quantity());
            total = total.add(itemWeight);
        }
        return total;
    }

    private ClubTierStrategy getClubStrategy(ClubTier tier) {
        if (tier == null) {
            throw ErrorCode.INVALID_CLUB_TIER.asException();
        }
        ClubTierStrategy strategy = clubStrategies.get(tier);
        if (strategy == null) {
            throw ErrorCode.INVALID_CLUB_TIER.asException();
        }
        return strategy;
    }

    private ShippingStrategy.ShippingResult calculateShipping(
            OrderRequest request, Weight totalWeight, ClubTierStrategy clubStrategy) {

        if (request.region() == null) {
            throw ErrorCode.INVALID_REGION.asException();
        }

        if (request.zipCode() == null || !request.zipCode().matches("\\d{8}")) {
            throw ErrorCode.INVALID_ZIP_CODE.asException();
        }

        if (request.shippingMethod() == null) {
            throw ErrorCode.INVALID_SHIPPING_METHOD.asException();
        }

        ShippingStrategy strategy = shippingStrategies.get(request.shippingMethod());
        if (strategy == null) {
            throw ErrorCode.INVALID_SHIPPING_METHOD.asException();
        }

        ShippingStrategy.ShippingContext context = new ShippingStrategy.ShippingContext(
            request.zipCode(), totalWeight, request.region()
        );

        boolean available;
        try {
            available = strategy.isAvailable(context);
        } catch (TransLogUnavailableException e) {
            throw e;
        }

        if (!available) {
            throw ErrorCode.SHIPPING_METHOD_UNAVAILABLE.asException();
        }

        ShippingStrategy.ShippingResult result = strategy.calculateShipping(context);

        if (clubStrategy.hasFreeShipping(request.shippingMethod())) {
            return new ShippingStrategy.ShippingResult(Money.zero(), result.days());
        }

        return result;
    }

    private Money calculateCouponDiscount(
            OrderRequest request,
            Money productsSubtotal,
            Money shippingCost,
            Map<String, Product> products) {

        if (request.coupon() == null) {
            return Money.zero();
        }

        CouponStrategy strategy = couponStrategies.get(request.coupon());
        if (strategy == null) {
            throw ErrorCode.INVALID_COUPON.asException();
        }

        boolean isFirstPurchase = !orderRepository.existsByCustomerCpfAndStatusIn(
            request.customer().cpf(),
            OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED
        );

        List<OrderItem> items = request.items().stream()
            .map(i -> new OrderItem(i.sku(), i.quantity()))
            .toList();

        CouponStrategy.CouponContext context = new CouponStrategy.CouponContext(
            productsSubtotal, shippingCost, items, products, isFirstPurchase
        );

        if (!strategy.isApplicable(context)) {
            throw ErrorCode.COUPON_NOT_APPLICABLE.asException();
        }

        Money discount = strategy.calculateDiscount(context);

        Money maxDiscount = productsSubtotal.percentage(BigDecimal.valueOf(30));
        return discount.min(maxDiscount);
    }

    private PaymentStrategy.PaymentResult calculatePayment(OrderRequest request, Money orderTotal) {
        if (request.paymentMethod() == null) {
            throw ErrorCode.INVALID_PAYMENT_METHOD.asException();
        }

        PaymentStrategy strategy = paymentStrategies.get(request.paymentMethod());
        if (strategy == null) {
            throw ErrorCode.INVALID_PAYMENT_METHOD.asException();
        }

        if (!strategy.isValidInstallments(request.getInstallments())) {
            throw ErrorCode.INVALID_INSTALLMENTS.asException();
        }

        PaymentStrategy.PaymentContext context = new PaymentStrategy.PaymentContext(
            orderTotal, request.getInstallments(), request.coupon() != null
        );

        if (!strategy.isAvailable(context)) {
            throw ErrorCode.PAYMENT_METHOD_UNAVAILABLE.asException();
        }

        return strategy.calculatePayment(context);
    }
}
