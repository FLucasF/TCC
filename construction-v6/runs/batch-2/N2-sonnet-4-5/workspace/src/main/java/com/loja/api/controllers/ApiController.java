package com.loja.api.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.loja.api.dto.*;
import com.loja.domain.entities.*;
import com.loja.domain.strategies.shipping.ShippingUnavailableException;
import com.loja.repositories.CustomerRepository;
import com.loja.repositories.NotificationRepository;
import com.loja.repositories.OrderRepository;
import com.loja.repositories.ProductRepository;
import com.loja.services.CheckoutService;
import com.loja.services.OrderService;
import com.loja.services.ValidationService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ApiController {
    private final Gson gson;
    private final CheckoutService checkoutService;
    private final OrderService orderService;
    private final ValidationService validationService;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final NotificationRepository notificationRepository;
    private final OrderRepository orderRepository;

    public ApiController(
            CheckoutService checkoutService,
            OrderService orderService,
            ValidationService validationService,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            NotificationRepository notificationRepository,
            OrderRepository orderRepository) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.checkoutService = checkoutService;
        this.orderService = orderService;
        this.validationService = validationService;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.notificationRepository = notificationRepository;
        this.orderRepository = orderRepository;
    }

    public void registerRoutes(Javalin app) {
        app.post("/checkout/summary", this::calculateSummary);
        app.post("/orders", this::createOrder);
        app.get("/orders/{id}", this::getOrder);
        app.post("/orders/{id}/payment", this::confirmPayment);
        app.post("/orders/{id}/shipment", this::shipOrder);
        app.post("/orders/{id}/delivery", this::deliverOrder);
        app.post("/orders/{id}/cancellation", this::cancelOrder);
        app.get("/products/{sku}", this::getProduct);
        app.get("/customers/{cpf}", this::getCustomer);
        app.get("/customers/{cpf}/notifications", this::getNotifications);
        app.post("/notifications/{id}/read", this::markNotificationAsRead);
    }

    private void calculateSummary(Context ctx) {
        try {
            CheckoutRequestDto dto = gson.fromJson(ctx.body(), CheckoutRequestDto.class);
            CheckoutService.CheckoutRequest request = parseCheckoutRequest(dto);

            validationService.validateCheckoutRequest(request);
            OrderSummary summary = checkoutService.calculateSummary(request);

            ctx.status(200).json(toSummaryDto(summary));

        } catch (ValidationService.ValidationException e) {
            handleValidationError(ctx, e);
        } catch (ShippingUnavailableException e) {
            ctx.status(503).json(new ErrorResponseDto("CARRIER_UNAVAILABLE"));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private void createOrder(Context ctx) {
        try {
            CheckoutRequestDto dto = gson.fromJson(ctx.body(), CheckoutRequestDto.class);
            CheckoutService.CheckoutRequest request = parseCheckoutRequest(dto);

            validationService.validateCheckoutRequest(request);
            OrderSummary summary = checkoutService.calculateSummary(request);

            List<OrderItem> items = dto.items.stream()
                    .map(i -> new OrderItem(i.sku, i.quantity))
                    .collect(Collectors.toList());

            Order order = orderService.createOrder(
                    dto.customer.cpf,
                    dto.customer.email,
                    items,
                    summary
            );

            ctx.status(201).json(toOrderDto(order));

        } catch (ValidationService.ValidationException e) {
            handleValidationError(ctx, e);
        } catch (ShippingUnavailableException e) {
            ctx.status(503).json(new ErrorResponseDto("CARRIER_UNAVAILABLE"));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private void getOrder(Context ctx) {
        try {
            String orderId = ctx.pathParam("id");
            Order order = orderRepository.findById(orderId);

            if (order == null) {
                ctx.status(404).json(new ErrorResponseDto("ORDER_NOT_FOUND"));
                return;
            }

            ctx.status(200).json(toOrderDto(order));

        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private void confirmPayment(Context ctx) {
        try {
            String orderId = ctx.pathParam("id");
            PaymentRequestDto dto = gson.fromJson(ctx.body(), PaymentRequestDto.class);

            if (dto.transactionId == null || dto.transactionId.isBlank() ||
                    dto.paidAt == null || dto.paidAt.isBlank()) {
                ctx.status(422).json(new ErrorResponseDto("INVALID_ORDER"));
                return;
            }

            Instant paidAt = Instant.parse(dto.paidAt);
            Order order = orderService.confirmPayment(orderId, dto.transactionId, paidAt);

            ctx.status(200).json(toOrderDto(order));

        } catch (OrderService.OrderExpiredException e) {
            Order order = orderRepository.findById(ctx.pathParam("id"));
            ctx.status(409).json(new ErrorResponseDto("ORDER_EXPIRED"));
        } catch (OrderService.InsufficientStockException e) {
            ctx.status(409).json(new ErrorResponseDto("INSUFFICIENT_STOCK"));
        } catch (OrderService.PaymentException e) {
            if (e.getMessage().contains("not found")) {
                ctx.status(404).json(new ErrorResponseDto("ORDER_NOT_FOUND"));
            } else if (e.getMessage().contains("cannot be paid")) {
                ctx.status(409).json(new ErrorResponseDto("OPERATION_NOT_ALLOWED"));
            } else {
                ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
            }
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private void shipOrder(Context ctx) {
        try {
            String orderId = ctx.pathParam("id");
            ShipmentRequestDto dto = gson.fromJson(ctx.body(), ShipmentRequestDto.class);

            if (dto.trackingCode == null || dto.trackingCode.isBlank()) {
                ctx.status(422).json(new ErrorResponseDto("INVALID_ORDER"));
                return;
            }

            Order order = orderService.shipOrder(orderId, dto.trackingCode);
            ctx.status(200).json(toOrderDto(order));

        } catch (OrderService.ShipmentException e) {
            if (e.getMessage().contains("not found")) {
                ctx.status(404).json(new ErrorResponseDto("ORDER_NOT_FOUND"));
            } else {
                ctx.status(409).json(new ErrorResponseDto("OPERATION_NOT_ALLOWED"));
            }
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private void deliverOrder(Context ctx) {
        try {
            String orderId = ctx.pathParam("id");
            Order order = orderService.deliverOrder(orderId);
            ctx.status(200).json(toOrderDto(order));

        } catch (OrderService.DeliveryException e) {
            if (e.getMessage().contains("not found")) {
                ctx.status(404).json(new ErrorResponseDto("ORDER_NOT_FOUND"));
            } else {
                ctx.status(409).json(new ErrorResponseDto("OPERATION_NOT_ALLOWED"));
            }
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private void cancelOrder(Context ctx) {
        try {
            String orderId = ctx.pathParam("id");
            Order order = orderService.cancelOrder(orderId);
            ctx.status(200).json(toOrderDto(order));

        } catch (OrderService.CancellationException e) {
            if (e.getMessage().contains("not found")) {
                ctx.status(404).json(new ErrorResponseDto("ORDER_NOT_FOUND"));
            } else {
                ctx.status(409).json(new ErrorResponseDto("OPERATION_NOT_ALLOWED"));
            }
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private void getProduct(Context ctx) {
        try {
            String sku = ctx.pathParam("sku");
            Product product = productRepository.findBySku(sku);

            if (product == null) {
                ctx.status(404).json(new ErrorResponseDto("PRODUCT_NOT_FOUND"));
                return;
            }

            ProductResponseDto dto = new ProductResponseDto();
            dto.sku = product.getSku();
            dto.name = product.getName();
            dto.price = product.getPrice().toDouble();
            dto.weightKg = product.getWeightKg();
            dto.stock = product.getStock();
            dto.discontinued = product.isDiscontinued();

            ctx.status(200).json(dto);

        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private void getCustomer(Context ctx) {
        try {
            String cpf = ctx.pathParam("cpf");
            Customer customer = customerRepository.findByCpf(cpf);

            if (customer == null) {
                ctx.status(404).json(new ErrorResponseDto("CUSTOMER_NOT_FOUND"));
                return;
            }

            CustomerResponseDto dto = new CustomerResponseDto();
            dto.cpf = customer.getCpf();
            dto.creditBalance = customer.getCreditBalance().toDouble();

            ctx.status(200).json(dto);

        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private void getNotifications(Context ctx) {
        try {
            String cpf = ctx.pathParam("cpf");

            if (!customerRepository.exists(cpf)) {
                ctx.status(404).json(new ErrorResponseDto("CUSTOMER_NOT_FOUND"));
                return;
            }

            List<Notification> notifications = notificationRepository.findByCustomerCpf(cpf);
            List<NotificationResponseDto> dtos = notifications.stream()
                    .map(n -> {
                        NotificationResponseDto dto = new NotificationResponseDto();
                        dto.id = n.getId();
                        dto.type = n.getType().name();
                        dto.orderId = n.getOrderId();
                        dto.status = n.getStatus().name();
                        return dto;
                    })
                    .collect(Collectors.toList());

            ctx.status(200).json(dtos);

        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private void markNotificationAsRead(Context ctx) {
        try {
            long notificationId = Long.parseLong(ctx.pathParam("id"));
            Notification notification = notificationRepository.findById(notificationId);

            if (notification == null) {
                ctx.status(404).json(new ErrorResponseDto("NOTIFICATION_NOT_FOUND"));
                return;
            }

            notification.markAsRead();
            notificationRepository.update(notification);

            NotificationResponseDto dto = new NotificationResponseDto();
            dto.id = notification.getId();
            dto.type = notification.getType().name();
            dto.orderId = notification.getOrderId();
            dto.status = notification.getStatus().name();

            ctx.status(200).json(dto);

        } catch (NumberFormatException e) {
            ctx.status(404).json(new ErrorResponseDto("NOTIFICATION_NOT_FOUND"));
        } catch (Exception e) {
            ctx.status(500).json(new ErrorResponseDto("INTERNAL_ERROR"));
        }
    }

    private CheckoutService.CheckoutRequest parseCheckoutRequest(CheckoutRequestDto dto) {
        List<OrderItem> items = dto.items.stream()
                .map(i -> new OrderItem(i.sku, i.quantity))
                .collect(Collectors.toList());

        return new CheckoutService.CheckoutRequest(
                dto.customer.cpf,
                items,
                dto.zipCode,
                dto.region != null ? Region.valueOf(dto.region) : null,
                dto.shippingMethod != null ? ShippingMethod.valueOf(dto.shippingMethod) : null,
                dto.coupon != null && !dto.coupon.isBlank() ? CouponCode.valueOf(dto.coupon) : null,
                dto.paymentMethod != null ? PaymentMethod.valueOf(dto.paymentMethod) : null,
                dto.installments != null ? dto.installments : 1,
                dto.clubTier != null ? ClubTier.valueOf(dto.clubTier) : null
        );
    }

    private SummaryResponseDto toSummaryDto(OrderSummary summary) {
        SummaryResponseDto dto = new SummaryResponseDto();
        dto.productsSubtotal = summary.getProductsSubtotal().toDouble();
        dto.couponDiscount = summary.getCouponDiscount().toDouble();
        dto.shippingCost = summary.getShippingCost().toDouble();
        dto.deliveryDays = summary.getDeliveryDays();
        dto.insurance = summary.getInsurance().toDouble();
        dto.paymentAdjustment = summary.getPaymentAdjustment().toDouble();
        dto.finalTotal = summary.getFinalTotal().toDouble();
        dto.installments = summary.getInstallments();
        dto.installmentAmount = summary.getInstallmentAmount().toDouble();
        dto.nextPurchaseCredit = summary.getNextPurchaseCredit().toDouble();
        dto.gift = summary.isGift();
        return dto;
    }

    private OrderResponseDto toOrderDto(Order order) {
        OrderResponseDto dto = new OrderResponseDto();
        dto.id = order.getId();
        dto.status = order.getStatus().name();
        dto.createdAt = DateTimeFormatter.ISO_INSTANT.format(order.getCreatedAt());
        dto.summary = toSummaryDto(order.getSummary());
        dto.items = order.getItems().stream()
                .map(i -> {
                    OrderResponseDto.OrderItemResponseDto itemDto = new OrderResponseDto.OrderItemResponseDto();
                    itemDto.sku = i.getSku();
                    itemDto.quantity = i.getQuantity();
                    return itemDto;
                })
                .collect(Collectors.toList());
        dto.trackingCode = order.getTrackingCode();
        return dto;
    }

    private void handleValidationError(Context ctx, ValidationService.ValidationException e) {
        int status = switch (e.getErrorCode()) {
            case "CARRIER_UNAVAILABLE" -> 503;
            default -> 422;
        };
        ctx.status(status).json(new ErrorResponseDto(e.getErrorCode()));
    }
}
