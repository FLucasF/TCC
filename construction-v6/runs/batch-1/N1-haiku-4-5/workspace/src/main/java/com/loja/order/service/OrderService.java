package com.loja.order.service;

import com.loja.order.domain.*;
import com.loja.order.dto.*;
import com.loja.order.model.*;
import com.loja.order.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class OrderService {
  private final OrderRepository orderRepository;
  private final ProductRepository productRepository;
  private final CustomerRepository customerRepository;
  private final NotificationRepository notificationRepository;
  private final PricingService pricingService;
  private final TransLogClient transLogClient;

  public OrderService(
    OrderRepository orderRepository,
    ProductRepository productRepository,
    CustomerRepository customerRepository,
    NotificationRepository notificationRepository,
    PricingService pricingService,
    TransLogClient transLogClient
  ) {
    this.orderRepository = orderRepository;
    this.productRepository = productRepository;
    this.customerRepository = customerRepository;
    this.notificationRepository = notificationRepository;
    this.pricingService = pricingService;
    this.transLogClient = transLogClient;
  }

  public SummaryResponse calculateSummary(OrderRequest request) throws ValidationException, ServiceException {
    validateRequest(request);

    List<PricingService.SummaryItem> items = new ArrayList<>();
    BigDecimal totalWeight = BigDecimal.ZERO;

    for (OrderRequest.ItemRequest itemReq : request.items()) {
      Product product = productRepository.findById(itemReq.sku())
        .orElseThrow(() -> new ValidationException("UNKNOWN_PRODUCT"));

      if (product.isDiscontinued()) {
        throw new ValidationException("PRODUCT_DISCONTINUED");
      }

      items.add(new PricingService.SummaryItem(
        itemReq.sku(),
        itemReq.quantity(),
        product.getPrice()
      ));

      totalWeight = totalWeight.add(
        product.getWeightKg().multiply(new BigDecimal(itemReq.quantity()))
      );
    }

    ShippingMethod shippingMethod = parseShippingMethod(request.shippingMethod());
    Region region = parseRegion(request.region());

    checkShippingAvailability(shippingMethod, totalWeight, request.zipCode());

    BigDecimal shippingCost = BigDecimal.ZERO;
    int deliveryDays = 0;

    if (shippingMethod == ShippingMethod.STORE_PICKUP) {
      deliveryDays = 1;
    } else if (shippingMethod == ShippingMethod.COURIER) {
      shippingCost = new BigDecimal("18.00");
      deliveryDays = 0;
    } else {
      try {
        String service = shippingMethod == ShippingMethod.ECONOMY ? "STD" : "EXP";
        TransLogClient.QuoteResponse quote = transLogClient.quote(
          request.zipCode(),
          totalWeight.intValue() * 1000,
          service
        );
        shippingCost = quote.price();
        deliveryDays = quote.etaDays() + region.getCarrierDaysAddition(shippingMethod);
      } catch (TransLogClient.ZipCodeNotServedException e) {
        throw new ValidationException("SHIPPING_METHOD_UNAVAILABLE");
      } catch (TransLogClient.TransLogUnavailableException e) {
        throw new ServiceException("CARRIER_UNAVAILABLE");
      } catch (Exception e) {
        throw new ServiceException("CARRIER_UNAVAILABLE");
      }
    }

    ClubTier clubTier = parseClubTier(request.clubTier());
    PaymentMethod paymentMethod = parsePaymentMethod(request.paymentMethod());
    int installments = request.installments() != null ? request.installments() : 1;

    validatePaymentMethod(paymentMethod, installments);

    boolean isFirstPurchase = !orderRepository.hasCompletedOrders(request.customer().cpf());

    BigDecimal productsSubtotal = calculateProductsSubtotal(items);

    validateCoupon(request.coupon(), items, isFirstPurchase);

    OrderSummary summary = pricingService.calculateSummary(
      items,
      request.coupon(),
      shippingMethod,
      shippingCost,
      deliveryDays,
      region,
      paymentMethod,
      installments,
      clubTier,
      isFirstPurchase
    );

    if (paymentMethod == PaymentMethod.BANK_SLIP && summary.getFinalTotal().compareTo(new BigDecimal("1000.00")) > 0) {
      throw new ValidationException("PAYMENT_METHOD_UNAVAILABLE");
    }

    return new SummaryResponse(
      summary.getProductsSubtotal(),
      summary.getCouponDiscount(),
      summary.getShippingCost(),
      summary.getDeliveryDays(),
      summary.getInsurance(),
      summary.getPaymentAdjustment(),
      summary.getFinalTotal(),
      summary.getInstallments(),
      summary.getInstallmentAmount(),
      summary.getNextPurchaseCredit(),
      summary.isGift()
    );
  }

  @Transactional
  public OrderResponse createOrder(OrderRequest request) throws ValidationException, ServiceException {
    SummaryResponse summary = calculateSummary(request);

    Order order = new Order(
      request.customer().cpf(),
      request.customer().email(),
      parseRegion(request.region()),
      parseShippingMethod(request.shippingMethod()),
      request.zipCode(),
      parsePaymentMethod(request.paymentMethod()),
      request.installments() != null ? request.installments() : 1,
      parseClubTier(request.clubTier()),
      request.coupon()
    );

    for (OrderRequest.ItemRequest itemReq : request.items()) {
      Product product = productRepository.findById(itemReq.sku())
        .orElseThrow(() -> new ValidationException("UNKNOWN_PRODUCT"));
      order.addItem(itemReq.sku(), itemReq.quantity(), product.getPrice());
    }

    OrderSummary summaryModel = new OrderSummary(
      summary.productsSubtotal(),
      summary.couponDiscount(),
      summary.shippingCost(),
      summary.deliveryDays(),
      summary.insurance(),
      summary.paymentAdjustment(),
      summary.finalTotal(),
      summary.installments(),
      summary.installmentAmount(),
      summary.nextPurchaseCredit(),
      summary.gift()
    );

    order.setSummary(summaryModel);
    Order savedOrder = orderRepository.save(order);

    return convertToResponse(savedOrder);
  }

  @Transactional
  public OrderResponse processPayment(Long orderId, PaymentRequest paymentRequest) throws ValidationException, NotFoundException, OperationNotAllowedException {
    if (paymentRequest.transactionId() == null || paymentRequest.paidAt() == null) {
      throw new ValidationException("INVALID_ORDER");
    }

    Order order = orderRepository.findByIdForUpdate(orderId)
      .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND"));

    if (order.getTransactionId() != null && order.getTransactionId().equals(paymentRequest.transactionId())) {
      return convertToResponse(order);
    }

    if (!order.getStatus().equals(OrderStatus.AWAITING_PAYMENT)) {
      throw new OperationNotAllowedException("OPERATION_NOT_ALLOWED");
    }

    if (order.isExpiredForPayment()) {
      order.cancel();
      orderRepository.save(order);
      createNotification(order.getCustomerCpf(), Notification.Type.ORDER_CANCELLED, orderId);
      throw new OperationNotAllowedException("ORDER_EXPIRED");
    }

    checkStock(order);

    order.markAsPaid(paymentRequest.transactionId(), paymentRequest.paidAt());
    decreaseStock(order);

    Customer customer = customerRepository.findByCpfForUpdate(order.getCustomerCpf());
    if (customer == null) {
      customer = new Customer(order.getCustomerCpf(), order.getCustomerEmail());
    }

    customer.addCredit(order.getSummary().getNextPurchaseCredit());
    customerRepository.save(customer);

    Order savedOrder = orderRepository.save(order);
    createNotification(order.getCustomerCpf(), Notification.Type.PAYMENT_APPROVED, orderId);

    return convertToResponse(savedOrder);
  }

  @Transactional
  public OrderResponse shipOrder(Long orderId, ShipmentRequest shipmentRequest) throws ValidationException, NotFoundException, OperationNotAllowedException {
    if (shipmentRequest.trackingCode() == null) {
      throw new ValidationException("INVALID_ORDER");
    }

    Order order = orderRepository.findByIdForUpdate(orderId)
      .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND"));

    if (!order.canBeShipped()) {
      throw new OperationNotAllowedException("OPERATION_NOT_ALLOWED");
    }

    order.markAsShipped(shipmentRequest.trackingCode());
    Order savedOrder = orderRepository.save(order);
    createNotification(order.getCustomerCpf(), Notification.Type.ORDER_SHIPPED, orderId);

    return convertToResponse(savedOrder);
  }

  @Transactional
  public OrderResponse deliverOrder(Long orderId) throws NotFoundException, OperationNotAllowedException {
    Order order = orderRepository.findByIdForUpdate(orderId)
      .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND"));

    if (!order.canBeDelivered()) {
      throw new OperationNotAllowedException("OPERATION_NOT_ALLOWED");
    }

    order.markAsDelivered();
    Order savedOrder = orderRepository.save(order);

    return convertToResponse(savedOrder);
  }

  @Transactional
  public OrderResponse cancelOrder(Long orderId) throws NotFoundException, OperationNotAllowedException {
    Order order = orderRepository.findByIdForUpdate(orderId)
      .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND"));

    if (!order.canBeCancelled()) {
      throw new OperationNotAllowedException("OPERATION_NOT_ALLOWED");
    }

    if (order.getStatus() == OrderStatus.PAID) {
      increaseStock(order);
      Customer customer = customerRepository.findByCpfForUpdate(order.getCustomerCpf());
      if (customer != null) {
        customer.removeCredit(order.getSummary().getNextPurchaseCredit());
        customerRepository.save(customer);
      }
    }

    order.cancel();
    Order savedOrder = orderRepository.save(order);
    createNotification(order.getCustomerCpf(), Notification.Type.ORDER_CANCELLED, orderId);

    return convertToResponse(savedOrder);
  }

  public OrderResponse getOrder(Long orderId) throws NotFoundException {
    Order order = orderRepository.findById(orderId)
      .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND"));
    return convertToResponse(order);
  }

  public ProductDto getProduct(String sku) throws NotFoundException {
    Product product = productRepository.findById(sku)
      .orElseThrow(() -> new NotFoundException("PRODUCT_NOT_FOUND"));

    return new ProductDto(
      product.getSku(),
      product.getName(),
      product.getPrice(),
      product.getWeightKg(),
      product.getStock(),
      product.isDiscontinued()
    );
  }

  public CustomerDto getCustomer(String cpf) throws NotFoundException {
    Customer customer = customerRepository.findById(cpf)
      .orElseThrow(() -> new NotFoundException("CUSTOMER_NOT_FOUND"));

    return new CustomerDto(
      customer.getCpf(),
      customer.getCreditBalance()
    );
  }

  private BigDecimal calculateProductsSubtotal(List<PricingService.SummaryItem> items) {
    return items.stream()
      .map(item -> item.price().multiply(new BigDecimal(item.quantity())))
      .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private void validateRequest(OrderRequest request) throws ValidationException {
    if (request.customer() == null || request.customer().cpf() == null || request.customer().email() == null) {
      throw new ValidationException("INVALID_ORDER");
    }

    if (request.items() == null || request.items().isEmpty()) {
      throw new ValidationException("INVALID_ORDER");
    }

    for (OrderRequest.ItemRequest item : request.items()) {
      if (item.sku() == null || item.quantity() == null || item.quantity() <= 0) {
        throw new ValidationException("INVALID_ORDER");
      }
    }

    if (request.zipCode() == null || request.zipCode().isEmpty()) {
      throw new ValidationException("INVALID_ZIP_CODE");
    }

    for (OrderRequest.ItemRequest itemReq : request.items()) {
      if (request.items().stream().filter(i -> i.sku().equals(itemReq.sku())).count() > 1) {
        throw new ValidationException("INVALID_ORDER");
      }

      Product product = productRepository.findById(itemReq.sku()).orElse(null);
      if (product == null) {
        throw new ValidationException("UNKNOWN_PRODUCT");
      }

      if (!product.hasEnoughStock(itemReq.quantity())) {
        throw new ValidationException("INSUFFICIENT_STOCK");
      }
    }
  }

  private void validateCoupon(String coupon, List<PricingService.SummaryItem> items, boolean isFirstPurchase)
    throws ValidationException {
    if (coupon == null || coupon.isEmpty()) {
      return;
    }

    Optional<Coupon> couponOpt = Coupon.fromCode(coupon);
    if (!couponOpt.isPresent()) {
      throw new ValidationException("INVALID_COUPON");
    }

    Coupon couponEnum = couponOpt.get();
    BigDecimal subtotal = items.stream()
      .map(item -> item.price().multiply(new BigDecimal(item.quantity())))
      .reduce(BigDecimal.ZERO, BigDecimal::add);

    List<Coupon.OrderItem> couponItems = items.stream()
      .map(item -> new Coupon.OrderItem(item.sku(), item.quantity(), item.price()))
      .toList();

    Coupon.OrderContext ctx = new Coupon.OrderContext(
      couponItems,
      isFirstPurchase,
      ShippingMethod.STORE_PICKUP,
      BigDecimal.ZERO
    );

    if (couponEnum.calculateDiscount(subtotal, ctx).isEmpty()) {
      throw new ValidationException("COUPON_NOT_APPLICABLE");
    }
  }

  private void validatePaymentMethod(PaymentMethod method, int installments)
    throws ValidationException {
    if (installments < 1 || installments > method.getMaxInstallments()) {
      throw new ValidationException("INVALID_INSTALLMENTS");
    }
  }

  private void checkShippingAvailability(ShippingMethod method, BigDecimal totalWeight, String zipCode)
    throws ValidationException {
    if (method == ShippingMethod.COURIER && totalWeight.compareTo(new BigDecimal("5")) > 0) {
      throw new ValidationException("SHIPPING_METHOD_UNAVAILABLE");
    }
  }

  private void checkStock(Order order) throws OperationNotAllowedException {
    for (OrderItem item : order.getItems()) {
      Product product = productRepository.findById(item.getSku())
        .orElseThrow();

      if (!product.hasEnoughStock(item.getQuantity())) {
        throw new OperationNotAllowedException("INSUFFICIENT_STOCK");
      }
    }
  }

  private void decreaseStock(Order order) {
    for (OrderItem item : order.getItems()) {
      Product product = productRepository.findBySkuForUpdate(item.getSku());
      product.decreaseStock(item.getQuantity());
      productRepository.save(product);
    }
  }

  private void increaseStock(Order order) {
    for (OrderItem item : order.getItems()) {
      Product product = productRepository.findBySkuForUpdate(item.getSku());
      product.increaseStock(item.getQuantity());
      productRepository.save(product);
    }
  }

  private void createNotification(String cpf, Notification.Type type, Long orderId) {
    Optional<Notification> existing = notificationRepository.findByOrderIdAndType(orderId, type, cpf);
    if (existing.isEmpty()) {
      Notification notification = new Notification(cpf, type, orderId);
      notificationRepository.save(notification);
    }
  }

  private ClubTier parseClubTier(String tier) throws ValidationException {
    if (tier == null) {
      throw new ValidationException("INVALID_CLUB_TIER");
    }
    try {
      return ClubTier.valueOf(tier);
    } catch (IllegalArgumentException e) {
      throw new ValidationException("INVALID_CLUB_TIER");
    }
  }

  private Region parseRegion(String region) throws ValidationException {
    if (region == null) {
      throw new ValidationException("INVALID_REGION");
    }
    try {
      return Region.valueOf(region);
    } catch (IllegalArgumentException e) {
      throw new ValidationException("INVALID_REGION");
    }
  }

  private ShippingMethod parseShippingMethod(String method) throws ValidationException {
    if (method == null) {
      throw new ValidationException("INVALID_SHIPPING_METHOD");
    }
    try {
      return ShippingMethod.valueOf(method);
    } catch (IllegalArgumentException e) {
      throw new ValidationException("INVALID_SHIPPING_METHOD");
    }
  }

  private PaymentMethod parsePaymentMethod(String method) throws ValidationException {
    if (method == null) {
      throw new ValidationException("INVALID_PAYMENT_METHOD");
    }
    try {
      return PaymentMethod.valueOf(method);
    } catch (IllegalArgumentException e) {
      throw new ValidationException("INVALID_PAYMENT_METHOD");
    }
  }

  private OrderResponse convertToResponse(Order order) {
    List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
      .map(item -> new OrderResponse.OrderItemResponse(item.getSku(), item.getQuantity()))
      .toList();

    OrderSummary summary = order.getSummary();
    OrderResponse.OrderSummaryResponse summaryResponse = new OrderResponse.OrderSummaryResponse(
      summary.getProductsSubtotal(),
      summary.getCouponDiscount(),
      summary.getShippingCost(),
      summary.getDeliveryDays(),
      summary.getInsurance(),
      summary.getPaymentAdjustment(),
      summary.getFinalTotal(),
      summary.getInstallments(),
      summary.getInstallmentAmount(),
      summary.getNextPurchaseCredit(),
      summary.isGift()
    );

    return new OrderResponse(
      order.getId(),
      order.getStatus().name(),
      order.getCreatedAt(),
      summaryResponse,
      items,
      order.getTrackingCode()
    );
  }

  public static class ValidationException extends Exception {
    public ValidationException(String message) {
      super(message);
    }
  }

  public static class NotFoundException extends Exception {
    public NotFoundException(String message) {
      super(message);
    }
  }

  public static class OperationNotAllowedException extends Exception {
    public OperationNotAllowedException(String message) {
      super(message);
    }
  }

  public static class ServiceException extends Exception {
    public ServiceException(String message) {
      super(message);
    }
  }

  public record ProductDto(
    String sku,
    String name,
    BigDecimal price,
    BigDecimal weightKg,
    int stock,
    boolean discontinued
  ) {}

  public record CustomerDto(
    String cpf,
    BigDecimal creditBalance
  ) {}
}
