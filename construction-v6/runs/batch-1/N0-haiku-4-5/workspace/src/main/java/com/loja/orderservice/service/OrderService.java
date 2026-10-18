package com.loja.orderservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loja.orderservice.domain.entity.Customer;
import com.loja.orderservice.domain.entity.Notification;
import com.loja.orderservice.domain.entity.Order;
import com.loja.orderservice.domain.entity.Product;
import com.loja.orderservice.domain.enums.*;
import com.loja.orderservice.dto.CheckoutSummary;
import com.loja.orderservice.dto.CheckoutSummaryRequest;
import com.loja.orderservice.dto.OrderResponse;
import com.loja.orderservice.dto.PaymentNotificationRequest;
import com.loja.orderservice.exception.OrderServiceException;
import com.loja.orderservice.repository.CustomerRepository;
import com.loja.orderservice.repository.NotificationRepository;
import com.loja.orderservice.repository.OrderRepository;
import com.loja.orderservice.repository.ProductRepository;
import com.loja.orderservice.util.CurrencyUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class OrderService {
    private static final long PAYMENT_TIMEOUT_HOURS = 48;

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final ProductRepository productRepository;
    private final CheckoutService checkoutService;
    private final ObjectMapper objectMapper;

    @Transactional
    public OrderResponse createOrder(CheckoutSummaryRequest request) {
        CheckoutSummary summary = checkoutService.calculateSummary(request);

        Order order = Order.builder()
                .customerCpf(request.getCustomer().getCpf())
                .customerEmail(request.getCustomer().getEmail())
                .status(OrderStatus.AWAITING_PAYMENT)
                .createdAt(Instant.now())
                .summaryJson(serializeSummary(summary))
                .itemsJson(serializeItems(request.getItems()))
                .build();

        order = orderRepository.save(order);

        return mapOrderToResponse(order);
    }

    @Transactional
    public OrderResponse processPayment(String orderId, PaymentNotificationRequest paymentRequest) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            throw new OrderServiceException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND);
        }

        Order order = orderOpt.get();

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            if (order.getTransactionId() != null &&
                order.getTransactionId().equals(paymentRequest.getTransactionId())) {
                return mapOrderToResponse(order);
            }
            throw new OrderServiceException("OPERATION_NOT_ALLOWED", "Order is not awaiting payment", HttpStatus.CONFLICT);
        }

        if (paymentRequest.getTransactionId() == null || paymentRequest.getTransactionId().isEmpty() ||
            paymentRequest.getPaidAt() == null) {
            throw new OrderServiceException("INVALID_ORDER", "Missing transactionId or paidAt", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Instant paymentTime = paymentRequest.getPaidAt();
        Instant orderTime = order.getCreatedAt();
        long hoursDiff = (paymentTime.getEpochSecond() - orderTime.getEpochSecond()) / 3600;

        if (hoursDiff > PAYMENT_TIMEOUT_HOURS) {
            order.setStatus(OrderStatus.CANCELLED);
            orderRepository.save(order);
            throw new OrderServiceException("ORDER_EXPIRED", "Payment timeout", HttpStatus.CONFLICT);
        }

        CheckoutSummary summary = deserializeSummary(order.getSummaryJson());
        List<Map<String, Object>> items;
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemsList = objectMapper.readValue(order.getItemsJson(), List.class);
            items = itemsList;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize items", e);
        }

        List<Product> products = getProductsForOrder(items);

        for (int i = 0; i < items.size(); i++) {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = items.get(i);
            String sku = (String) item.get("sku");
            Long quantity = ((Number) item.get("quantity")).longValue();

            Optional<Product> productOpt = productRepository.findById(sku);
            if (productOpt.isEmpty() || productOpt.get().getStock() < quantity) {
                order.setStatus(OrderStatus.CANCELLED);
                orderRepository.save(order);
                createNotification(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId);
                throw new OrderServiceException("INSUFFICIENT_STOCK", "Insufficient stock", HttpStatus.CONFLICT);
            }
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(paymentTime);
        order.setTransactionId(paymentRequest.getTransactionId());
        order = orderRepository.save(order);

        for (int i = 0; i < items.size(); i++) {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = items.get(i);
            String sku = (String) item.get("sku");
            Long quantity = ((Number) item.get("quantity")).longValue();

            Product product = products.get(i);
            product.setStock(product.getStock() - quantity);
            productRepository.save(product);
        }

        String cpf = order.getCustomerCpf();
        Optional<Customer> customerOpt = customerRepository.findById(cpf);
        Customer customer;
        if (customerOpt.isEmpty()) {
            customer = Customer.builder()
                    .cpf(cpf)
                    .creditBalance(summary.getNextPurchaseCredit())
                    .build();
        } else {
            customer = customerOpt.get();
            customer.setCreditBalance(CurrencyUtils.add(customer.getCreditBalance(),
                    summary.getNextPurchaseCredit()));
        }
        customerRepository.save(customer);

        createNotification(cpf, NotificationType.PAYMENT_APPROVED, orderId);

        return mapOrderToResponse(order);
    }

    @Transactional
    public OrderResponse shipOrder(String orderId, String trackingCode) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            throw new OrderServiceException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND);
        }

        Order order = orderOpt.get();

        if (trackingCode == null || trackingCode.isEmpty()) {
            throw new OrderServiceException("INVALID_ORDER", "Tracking code is required", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        if (order.getStatus() != OrderStatus.PAID) {
            throw new OrderServiceException("OPERATION_NOT_ALLOWED", "Order is not paid", HttpStatus.CONFLICT);
        }

        order.setStatus(OrderStatus.SHIPPED);
        order.setTrackingCode(trackingCode);
        order = orderRepository.save(order);

        createNotification(order.getCustomerCpf(), NotificationType.ORDER_SHIPPED, orderId);

        return mapOrderToResponse(order);
    }

    @Transactional
    public OrderResponse deliverOrder(String orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            throw new OrderServiceException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND);
        }

        Order order = orderOpt.get();

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new OrderServiceException("OPERATION_NOT_ALLOWED", "Order is not shipped", HttpStatus.CONFLICT);
        }

        order.setStatus(OrderStatus.DELIVERED);
        order = orderRepository.save(order);

        return mapOrderToResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            throw new OrderServiceException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND);
        }

        Order order = orderOpt.get();

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.DELIVERED) {
            throw new OrderServiceException("OPERATION_NOT_ALLOWED", "Order cannot be cancelled after shipment", HttpStatus.CONFLICT);
        }

        if (order.getStatus() == OrderStatus.PAID) {
            List<Map<String, Object>> items;
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> itemsList = objectMapper.readValue(order.getItemsJson(), List.class);
                items = itemsList;
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize items", e);
            }

            for (var item : items) {
                String sku = (String) item.get("sku");
                Long quantity = ((Number) item.get("quantity")).longValue();

                Optional<Product> productOpt = productRepository.findById(sku);
                if (productOpt.isPresent()) {
                    Product product = productOpt.get();
                    product.setStock(product.getStock() + quantity);
                    productRepository.save(product);
                }
            }

            CheckoutSummary summary = deserializeSummary(order.getSummaryJson());
            String cpf = order.getCustomerCpf();
            Optional<Customer> customerOpt = customerRepository.findById(cpf);
            if (customerOpt.isPresent()) {
                Customer customer = customerOpt.get();
                customer.setCreditBalance(CurrencyUtils.subtract(customer.getCreditBalance(),
                        summary.getNextPurchaseCredit()));
                customerRepository.save(customer);
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);

        createNotification(order.getCustomerCpf(), NotificationType.ORDER_CANCELLED, orderId);

        return mapOrderToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        Optional<Order> orderOpt = orderRepository.findById(orderId);

        if (orderOpt.isEmpty()) {
            throw new OrderServiceException("ORDER_NOT_FOUND", "Order not found", HttpStatus.NOT_FOUND);
        }

        return mapOrderToResponse(orderOpt.get());
    }

    private List<Product> getProductsForOrder(List<Map<String, Object>> items) {
        List<Product> products = new java.util.ArrayList<>();
        for (var item : items) {
            String sku = (String) item.get("sku");
            Optional<Product> productOpt = productRepository.findById(sku);
            if (productOpt.isPresent()) {
                products.add(productOpt.get());
            }
        }
        return products;
    }

    private void createNotification(String cpf, NotificationType type, String orderId) {
        Notification notification = Notification.builder()
                .customerCpf(cpf)
                .type(type)
                .orderId(orderId)
                .status(NotificationStatus.NEW)
                .createdAt(Instant.now())
                .build();

        notificationRepository.save(notification);
    }

    private OrderResponse mapOrderToResponse(Order order) {
        CheckoutSummary summary = deserializeSummary(order.getSummaryJson());
        List<Map<String, Object>> itemsList;
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> items = objectMapper.readValue(order.getItemsJson(), List.class);
            itemsList = items;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize items", e);
        }

        List<OrderResponse.OrderItemResponse> items = itemsList.stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .sku((String) item.get("sku"))
                        .quantity(((Number) item.get("quantity")).longValue())
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .summary(summary)
                .items(items)
                .trackingCode(order.getTrackingCode())
                .build();
    }

    private String serializeSummary(CheckoutSummary summary) {
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize summary", e);
        }
    }

    private String serializeItems(List<CheckoutSummaryRequest.OrderItemRequest> items) {
        try {
            List<Map<String, Object>> itemMaps = items.stream()
                    .map(item -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("sku", item.getSku());
                        map.put("quantity", item.getQuantity());
                        return map;
                    })
                    .toList();
            return objectMapper.writeValueAsString(itemMaps);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize items", e);
        }
    }

    private CheckoutSummary deserializeSummary(String json) {
        try {
            return objectMapper.readValue(json, CheckoutSummary.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize summary", e);
        }
    }
}
