package br.com.loja.pedidos.checkout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import br.com.loja.pedidos.catalog.Product;
import br.com.loja.pedidos.catalog.ProductRepository;
import br.com.loja.pedidos.club.ClubTier;
import br.com.loja.pedidos.common.Coded;
import br.com.loja.pedidos.common.DomainException;
import br.com.loja.pedidos.common.Money;
import br.com.loja.pedidos.common.Region;
import br.com.loja.pedidos.coupon.Coupon;
import br.com.loja.pedidos.coupon.CouponContext;
import br.com.loja.pedidos.order.OrderRepository;
import br.com.loja.pedidos.payment.PaymentMethod;
import br.com.loja.pedidos.shipping.ShippingOption;
import br.com.loja.pedidos.shipping.ShippingQuote;
import br.com.loja.pedidos.shipping.ShippingRequest;

/** Calcula o resumo da compra. É a única fonte dos valores, tanto do resumo quanto do pedido registrado. */
@Service
public class CheckoutService {

    private static final BigDecimal COUPON_CAP_RATE = new BigDecimal("0.30");

    private final ProductRepository products;
    private final OrderRepository orders;
    private final Map<String, ClubTier> clubTiers;
    private final Map<String, ShippingOption> shippingOptions;
    private final Map<String, Coupon> coupons;
    private final Map<String, PaymentMethod> paymentMethods;

    public CheckoutService(ProductRepository products, OrderRepository orders, List<ClubTier> clubTiers,
                           List<ShippingOption> shippingOptions, List<Coupon> coupons,
                           List<PaymentMethod> paymentMethods) {
        this.products = products;
        this.orders = orders;
        this.clubTiers = Coded.index(clubTiers);
        this.shippingOptions = Coded.index(shippingOptions);
        this.coupons = Coded.index(coupons);
        this.paymentMethods = Coded.index(paymentMethods);
    }

    public Summary calculate(CheckoutRequest request) {
        validateShape(request);
        List<CartLine> lines = resolveLines(request);

        ClubTier tier = clubTiers.get(request.clubTier());
        if (tier == null) {
            throw DomainException.unprocessable("INVALID_CLUB_TIER");
        }
        Region region = Region.parse(request.region());
        if (region == null) {
            throw DomainException.unprocessable("INVALID_REGION");
        }
        if (request.zipCode() == null || !request.zipCode().matches("\\d{8}")) {
            throw DomainException.unprocessable("INVALID_ZIP_CODE");
        }
        ShippingOption shipping = shippingOptions.get(request.shippingMethod());
        if (shipping == null) {
            throw DomainException.unprocessable("INVALID_SHIPPING_METHOD");
        }

        BigDecimal subtotal = Money.round(lines.stream().map(CartLine::total).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal weightKg = lines.stream().map(CartLine::weightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
        ShippingQuote quote = shipping.quote(new ShippingRequest(request.zipCode(), region, weightKg));
        BigDecimal shippingCost = tier.freeShipping(shipping) ? Money.ZERO : quote.cost();

        BigDecimal couponDiscount = couponDiscount(request, lines, subtotal, shipping, shippingCost);
        BigDecimal insurance = region.insuranceFor(subtotal);
        BigDecimal orderTotal = subtotal.subtract(couponDiscount).add(shippingCost).add(insurance);

        int installments = request.installments() == null ? 1 : request.installments();
        PaymentMethod payment = paymentMethods.get(request.paymentMethod());
        if (payment == null) {
            throw DomainException.unprocessable("INVALID_PAYMENT_METHOD");
        }
        if (!payment.allowsInstallments(installments)) {
            throw DomainException.unprocessable("INVALID_INSTALLMENTS");
        }
        if (!payment.availableFor(orderTotal)) {
            throw DomainException.unprocessable("PAYMENT_METHOD_UNAVAILABLE");
        }
        var charge = payment.charge(
                new PaymentMethod.PaymentContext(orderTotal, installments, request.coupon() != null));

        return new Summary(subtotal, couponDiscount, shippingCost, quote.days(), insurance,
                charge.finalTotal().subtract(orderTotal), charge.finalTotal(), installments,
                charge.installmentAmount(), tier.nextPurchaseCredit(subtotal), tier.gift(subtotal));
    }

    private BigDecimal couponDiscount(CheckoutRequest request, List<CartLine> lines, BigDecimal subtotal,
                                      ShippingOption shipping, BigDecimal shippingCost) {
        if (request.coupon() == null) {
            return Money.ZERO;
        }
        Coupon coupon = coupons.get(request.coupon());
        if (coupon == null) {
            throw DomainException.unprocessable("INVALID_COUPON");
        }
        String cpf = request.customer().cpf();
        BigDecimal discount = coupon.discount(new CouponContext(lines, subtotal, shipping, shippingCost,
                () -> !orders.hasPurchase(cpf)));
        BigDecimal cap = Money.percent(subtotal, COUPON_CAP_RATE);
        return discount.compareTo(cap) > 0 ? cap : discount;
    }

    private void validateShape(CheckoutRequest request) {
        boolean valid = request.customer() != null
                && notBlank(request.customer().cpf())
                && notBlank(request.customer().email())
                && request.items() != null
                && !request.items().isEmpty();
        if (valid) {
            Set<String> seen = new HashSet<>();
            for (var item : request.items()) {
                if (item == null || !notBlank(item.sku()) || item.quantity() == null || item.quantity() <= 0
                        || !seen.add(item.sku())) {
                    valid = false;
                    break;
                }
            }
        }
        if (!valid) {
            throw DomainException.unprocessable("INVALID_ORDER");
        }
    }

    private List<CartLine> resolveLines(CheckoutRequest request) {
        List<CartLine> lines = new ArrayList<>();
        for (var item : request.items()) {
            Product product = products.find(item.sku())
                    .orElseThrow(() -> DomainException.unprocessable("UNKNOWN_PRODUCT"));
            lines.add(new CartLine(product, item.quantity()));
        }
        if (lines.stream().anyMatch(line -> line.product().discontinued())) {
            throw DomainException.unprocessable("PRODUCT_DISCONTINUED");
        }
        if (lines.stream().anyMatch(line -> line.product().stock() < line.quantity())) {
            throw DomainException.unprocessable("INSUFFICIENT_STOCK");
        }
        return lines;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
