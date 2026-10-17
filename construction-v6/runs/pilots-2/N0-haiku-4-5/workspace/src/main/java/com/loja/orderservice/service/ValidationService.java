package com.loja.orderservice.service;

import com.loja.orderservice.dto.CustomerDTO;
import com.loja.orderservice.dto.OrderItemDTO;
import com.loja.orderservice.dto.OrderRequestDTO;
import com.loja.orderservice.entity.*;
import com.loja.orderservice.exception.OrderException;
import com.loja.orderservice.repository.OrderRepository;
import com.loja.orderservice.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class ValidationService {
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public ValidationService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    public void validateOrderRequest(OrderRequestDTO request) {
        // 1. Validate items, customer, and basic structure
        validateItemsAndCustomer(request);

        // 2. Check if products exist and valid
        validateProducts(request.getItems());

        // 3. Check club tier
        validateClubTier(request.getClubTier());

        // 4. Check region
        validateRegion(request.getRegion());

        // 5. Check zip code
        validateZipCode(request.getZipCode());

        // 6. Check shipping method
        validateShippingMethod(request.getShippingMethod());

        // 7. Check payment method
        validatePaymentMethod(request.getPaymentMethod());

        // 8. Check installments
        validateInstallments(request.getPaymentMethod(), request.getInstallments());

        // 9. Validate payment method availability
        validatePaymentMethodAvailability(request);

        // 10. Validate coupon
        validateCoupon(request, request.getItems());
    }

    private void validateItemsAndCustomer(OrderRequestDTO request) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new OrderException("INVALID_ORDER", "Cart is empty", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        CustomerDTO customer = request.getCustomer();
        if (customer == null || customer.getCpf() == null || customer.getEmail() == null) {
            throw new OrderException("INVALID_ORDER", "Customer CPF and email are required", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Set<String> seenSkus = new HashSet<>();
        for (OrderItemDTO item : request.getItems()) {
            if (item.getSku() == null) {
                throw new OrderException("INVALID_ORDER", "Item SKU is required", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            if (seenSkus.contains(item.getSku())) {
                throw new OrderException("INVALID_ORDER", "Duplicate SKU in cart", HttpStatus.UNPROCESSABLE_ENTITY);
            }
            seenSkus.add(item.getSku());

            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new OrderException("INVALID_ORDER", "Quantity must be positive", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private void validateProducts(List<OrderItemDTO> items) {
        for (OrderItemDTO item : items) {
            Optional<Product> product = productRepository.findById(item.getSku());

            if (product.isEmpty()) {
                throw new OrderException("UNKNOWN_PRODUCT", "Product not found: " + item.getSku(), HttpStatus.UNPROCESSABLE_ENTITY);
            }

            Product p = product.get();
            if (p.isDiscontinued()) {
                throw new OrderException("PRODUCT_DISCONTINUED", "Product is discontinued: " + item.getSku(), HttpStatus.UNPROCESSABLE_ENTITY);
            }

            if (p.getStock() < item.getQuantity()) {
                throw new OrderException("INSUFFICIENT_STOCK", "Insufficient stock for: " + item.getSku(), HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private void validateClubTier(ClubTier tier) {
        if (tier == null) {
            throw new OrderException("INVALID_CLUB_TIER", "Club tier is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }
        try {
            ClubTier.valueOf(tier.toString());
        } catch (IllegalArgumentException e) {
            throw new OrderException("INVALID_CLUB_TIER", "Invalid club tier", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validateRegion(Region region) {
        if (region == null) {
            throw new OrderException("INVALID_REGION", "Region is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validateZipCode(String zipCode) {
        if (zipCode == null || zipCode.length() != 8 || !zipCode.matches("\\d{8}")) {
            throw new OrderException("INVALID_ZIP_CODE", "Invalid ZIP code format", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validateShippingMethod(ShippingMethod method) {
        if (method == null) {
            throw new OrderException("INVALID_SHIPPING_METHOD", "Shipping method is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validatePaymentMethod(PaymentMethod method) {
        if (method == null) {
            throw new OrderException("INVALID_PAYMENT_METHOD", "Payment method is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }
    }

    private void validateInstallments(PaymentMethod method, Integer installments) {
        int inst = installments != null ? installments : 1;

        if (method == PaymentMethod.PIX || method == PaymentMethod.BANK_SLIP) {
            if (inst != 1) {
                throw new OrderException("INVALID_INSTALLMENTS", "PIX and BANK_SLIP only support 1 installment", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        } else if (method == PaymentMethod.CREDIT_CARD) {
            if (inst < 1 || inst > 12) {
                throw new OrderException("INVALID_INSTALLMENTS", "Credit card supports 1-12 installments", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }

    private void validatePaymentMethodAvailability(OrderRequestDTO request) {
        if (request.getPaymentMethod() == PaymentMethod.BANK_SLIP) {
            // Will check total in PricingService
        }
    }

    private void validateCoupon(OrderRequestDTO request, List<OrderItemDTO> items) {
        String coupon = request.getCoupon();
        if (coupon != null && !coupon.isEmpty()) {
            if (!isValidCoupon(coupon)) {
                throw new OrderException("INVALID_COUPON", "Invalid coupon code", HttpStatus.UNPROCESSABLE_ENTITY);
            }

            validateCouponApplicability(coupon, request, items);
        }
    }

    private boolean isValidCoupon(String coupon) {
        return coupon.equals("WELCOME10") || coupon.equals("MINUS50") ||
               coupon.equals("FREESHIPPING") || coupon.equals("BUY3PAY2");
    }

    public void validateCouponApplicability(String coupon, OrderRequestDTO request, List<OrderItemDTO> items) {
        if (coupon == null || coupon.isEmpty()) {
            return;
        }

        if ("WELCOME10".equals(coupon)) {
            List<Order> completedOrders = orderRepository.findCompletedOrdersByCpf(request.getCustomer().getCpf());
            if (!completedOrders.isEmpty()) {
                throw new OrderException("COUPON_NOT_APPLICABLE", "WELCOME10 only for first purchase", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        if ("FREESHIPPING".equals(coupon)) {
            if (request.getShippingMethod() == ShippingMethod.STORE_PICKUP) {
                throw new OrderException("COUPON_NOT_APPLICABLE", "FREESHIPPING not valid for STORE_PICKUP", HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }
    }
}
