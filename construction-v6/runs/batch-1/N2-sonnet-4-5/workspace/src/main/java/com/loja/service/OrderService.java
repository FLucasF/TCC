package com.loja.service;

import com.loja.domain.*;
import com.loja.dto.*;
import com.loja.exception.OrderException;
import com.loja.repository.*;
import com.loja.state.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderService implements OrderStateContext {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final CheckoutService checkoutService;
    private final Map<String, OrderState> states;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderPaymentRepository orderPaymentRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            NotificationRepository notificationRepository,
            CheckoutService checkoutService,
            List<OrderState> stateList) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderPaymentRepository = orderPaymentRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
        this.checkoutService = checkoutService;
        this.states = stateList.stream().collect(Collectors.toMap(OrderState::getStatusCode, s -> s));
    }

    @Transactional
    public OrderResponse createOrder(CheckoutRequest request) {
        SummaryResponse summary = checkoutService.calculateSummary(request);

        Customer customer = customerRepository.findById(request.getCustomer().getCpf())
            .orElse(new Customer(request.getCustomer().getCpf(),
                               request.getCustomer().getEmail(),
                               request.getClubTier()));
        customerRepository.save(customer);

        String orderId = UUID.randomUUID().toString();

        Order order = new Order();
        order.setId(orderId);
        order.setCustomerCpf(request.getCustomer().getCpf());
        order.setCustomerEmail(request.getCustomer().getEmail());
        order.setStatus("AWAITING_PAYMENT");
        order.setZipCode(request.getZipCode());
        order.setRegion(request.getRegion());
        order.setShippingMethod(request.getShippingMethod());
        order.setCoupon(request.getCoupon());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setInstallments(request.getInstallments() != null ? request.getInstallments() : 1);
        order.setClubTier(request.getClubTier());

        order.setProductsSubtotal(summary.getProductsSubtotal());
        order.setCouponDiscount(summary.getCouponDiscount());
        order.setShippingCost(summary.getShippingCost());
        order.setDeliveryDays(summary.getDeliveryDays());
        order.setInsurance(summary.getInsurance());
        order.setPaymentAdjustment(summary.getPaymentAdjustment());
        order.setFinalTotal(summary.getFinalTotal());
        order.setInstallmentAmount(summary.getInstallmentAmount());
        order.setNextPurchaseCredit(summary.getNextPurchaseCredit());
        order.setGift(summary.getGift());

        order.setCreatedAt(LocalDateTime.now());

        orderRepository.save(order);

        for (var item : request.getItems()) {
            orderItemRepository.save(new OrderItem(orderId, item.getSku(), item.getQuantity()));
        }

        return toOrderResponse(order, request.getItems());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        List<ItemRequest> itemRequests = items.stream()
            .map(item -> {
                ItemRequest ir = new ItemRequest();
                ir.setSku(item.getSku());
                ir.setQuantity(item.getQuantity());
                return ir;
            })
            .collect(Collectors.toList());

        return toOrderResponse(order, itemRequests);
    }

    @Transactional
    public OrderResponse processPayment(String orderId, PaymentRequest paymentRequest) {
        if (paymentRequest.getTransactionId() == null || paymentRequest.getPaidAt() == null) {
            throw new OrderException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        OrderState state = states.get(order.getStatus());
        order.setState(state);

        state.handlePayment(order, paymentRequest, this);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<ItemRequest> itemRequests = items.stream()
            .map(item -> {
                ItemRequest ir = new ItemRequest();
                ir.setSku(item.getSku());
                ir.setQuantity(item.getQuantity());
                return ir;
            })
            .collect(Collectors.toList());

        return toOrderResponse(order, itemRequests);
    }

    @Transactional
    public OrderResponse shipOrder(String orderId, ShipmentRequest shipmentRequest) {
        if (shipmentRequest.getTrackingCode() == null) {
            throw new OrderException("INVALID_ORDER", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        OrderState state = states.get(order.getStatus());
        order.setState(state);

        state.handleShipment(order, shipmentRequest, this);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<ItemRequest> itemRequests = items.stream()
            .map(item -> {
                ItemRequest ir = new ItemRequest();
                ir.setSku(item.getSku());
                ir.setQuantity(item.getQuantity());
                return ir;
            })
            .collect(Collectors.toList());

        return toOrderResponse(order, itemRequests);
    }

    @Transactional
    public OrderResponse deliverOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        OrderState state = states.get(order.getStatus());
        order.setState(state);

        state.handleDelivery(order, this);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<ItemRequest> itemRequests = items.stream()
            .map(item -> {
                ItemRequest ir = new ItemRequest();
                ir.setSku(item.getSku());
                ir.setQuantity(item.getQuantity());
                return ir;
            })
            .collect(Collectors.toList());

        return toOrderResponse(order, itemRequests);
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new OrderException("ORDER_NOT_FOUND", HttpStatus.NOT_FOUND));

        OrderState state = states.get(order.getStatus());
        order.setState(state);

        state.handleCancellation(order, this);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<ItemRequest> itemRequests = items.stream()
            .map(item -> {
                ItemRequest ir = new ItemRequest();
                ir.setSku(item.getSku());
                ir.setQuantity(item.getQuantity());
                return ir;
            })
            .collect(Collectors.toList());

        return toOrderResponse(order, itemRequests);
    }

    @Override
    public void saveOrder(Order order) {
        orderRepository.save(order);
    }

    @Override
    public void reserveStock(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        for (OrderItem item : items) {
            Product product = productRepository.findBySkuForUpdate(item.getSku())
                .orElseThrow(() -> new RuntimeException("Product not found"));

            if (product.getStock() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock");
            }

            product.setStock(product.getStock() - item.getQuantity());
            productRepository.save(product);
        }
    }

    @Override
    public void releaseStock(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        for (OrderItem item : items) {
            Product product = productRepository.findBySkuForUpdate(item.getSku())
                .orElseThrow(() -> new RuntimeException("Product not found"));

            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }

    @Override
    public void addCredit(String cpf, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        Customer customer = customerRepository.findByCpfForUpdate(cpf)
            .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setCreditBalance(customer.getCreditBalance().add(amount));
        customerRepository.save(customer);
    }

    @Override
    public void removeCredit(String cpf, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        Customer customer = customerRepository.findByCpfForUpdate(cpf)
            .orElseThrow(() -> new RuntimeException("Customer not found"));

        customer.setCreditBalance(customer.getCreditBalance().subtract(amount));
        customerRepository.save(customer);
    }

    @Override
    public void notifyCustomer(String cpf, String type, String orderId) {
        Notification notification = new Notification(cpf, type, orderId);
        notificationRepository.save(notification);
    }

    @Override
    public OrderPayment findPaymentByOrderId(String orderId) {
        return orderPaymentRepository.findByOrderId(orderId).orElse(null);
    }

    @Override
    public void savePayment(OrderPayment payment) {
        orderPaymentRepository.save(payment);
    }

    @Override
    public List<Order> findOrdersByCustomerCpfAndStatuses(String cpf, List<String> statuses) {
        return orderRepository.findByCustomerCpfAndStatusIn(cpf, statuses);
    }

    @Override
    public boolean isPaymentExpired(Order order, PaymentRequest paymentRequest) {
        Duration duration = Duration.between(order.getCreatedAt(), paymentRequest.getPaidAt());
        return duration.compareTo(Duration.ofHours(48)) > 0;
    }

    @Override
    public boolean hasStockForOrder(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());

        for (OrderItem item : items) {
            Product product = productRepository.findById(item.getSku())
                .orElse(null);

            if (product == null || product.getStock() < item.getQuantity()) {
                return false;
            }
        }

        return true;
    }

    private OrderResponse toOrderResponse(Order order, List<ItemRequest> items) {
        SummaryResponse summary = new SummaryResponse();
        summary.setProductsSubtotal(order.getProductsSubtotal());
        summary.setCouponDiscount(order.getCouponDiscount());
        summary.setShippingCost(order.getShippingCost());
        summary.setDeliveryDays(order.getDeliveryDays());
        summary.setInsurance(order.getInsurance());
        summary.setPaymentAdjustment(order.getPaymentAdjustment());
        summary.setFinalTotal(order.getFinalTotal());
        summary.setInstallments(order.getInstallments());
        summary.setInstallmentAmount(order.getInstallmentAmount());
        summary.setNextPurchaseCredit(order.getNextPurchaseCredit());
        summary.setGift(order.getGift());

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());
        response.setSummary(summary);
        response.setItems(items);
        response.setTrackingCode(order.getTrackingCode());

        return response;
    }
}
