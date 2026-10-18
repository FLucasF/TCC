package br.com.loja.checkout;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.loja.catalog.Product;
import br.com.loja.catalog.ProductRepository;
import br.com.loja.club.ClubTier;
import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;
import br.com.loja.common.Money;
import br.com.loja.coupon.Coupon;
import br.com.loja.coupon.CouponContext;
import br.com.loja.order.OrderRepository;
import br.com.loja.payment.PaymentOption;
import br.com.loja.payment.PaymentOption.PaymentCharge;
import br.com.loja.payment.PaymentOption.PaymentContext;
import br.com.loja.shipping.Region;
import br.com.loja.shipping.ShippingOption;
import br.com.loja.shipping.ShippingQuote;
import br.com.loja.shipping.ShippingRequest;

/** Calcula o resumo da compra. É a única fonte dos valores, tanto do resumo quanto do pedido registrado. */
@Service
public class CheckoutService {

    private static final String COUPON_CAP_RATE = "0.30";

    private final ProductRepository products;
    private final OrderRepository orders;
    private final Map<String, ShippingOption> shippingOptions;
    private final Map<String, Coupon> coupons;
    private final Map<String, ClubTier> tiers;
    private final Map<String, PaymentOption> paymentOptions;

    CheckoutService(ProductRepository products, OrderRepository orders, List<ShippingOption> shippingOptions,
                    List<Coupon> coupons, List<ClubTier> tiers, List<PaymentOption> paymentOptions) {
        this.products = products;
        this.orders = orders;
        this.shippingOptions = index(shippingOptions, ShippingOption::code);
        this.coupons = index(coupons, Coupon::code);
        this.tiers = index(tiers, ClubTier::code);
        this.paymentOptions = index(paymentOptions, PaymentOption::code);
    }

    public OrderSummary summarize(CheckoutRequest request) {
        validateStructure(request);
        List<CartLine> lines = loadLines(request.items());

        ClubTier tier = find(tiers, request.clubTier(), ErrorCode.INVALID_CLUB_TIER);
        Region region = Region.parse(request.region()).orElseThrow(() -> new ApiException(ErrorCode.INVALID_REGION));
        String zip = request.zipCode();
        if (zip == null || !zip.matches("\\d{8}")) {
            throw new ApiException(ErrorCode.INVALID_ZIP_CODE);
        }
        ShippingOption shipping = find(shippingOptions, request.shippingMethod(), ErrorCode.INVALID_SHIPPING_METHOD);

        BigDecimal subtotal = lines.stream().map(CartLine::total).reduce(Money.ZERO, BigDecimal::add);
        BigDecimal weight = lines.stream()
                .map(l -> l.product().weightKg().multiply(BigDecimal.valueOf(l.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ShippingQuote quote = shipping.quote(new ShippingRequest(zip, region, weight));
        BigDecimal shippingCost = tier.waivesFreight() && shipping.freightWaivableByClub() ? Money.ZERO
                : quote.cost();

        BigDecimal couponDiscount = Money.ZERO;
        if (request.coupon() != null) {
            Coupon coupon = find(coupons, request.coupon(), ErrorCode.INVALID_COUPON);
            boolean firstPurchase = !orders.hasPurchaseByCustomer(request.customer().cpf());
            BigDecimal raw = coupon.discount(new CouponContext(lines, subtotal, shipping, shippingCost, firstPurchase));
            couponDiscount = Money.round(raw).min(Money.percent(subtotal, COUPON_CAP_RATE));
        }

        PaymentOption payment = find(paymentOptions, request.paymentMethod(), ErrorCode.INVALID_PAYMENT_METHOD);
        int installments = request.installments() == null ? 1 : request.installments();
        payment.validateInstallments(installments);

        BigDecimal insurance = region.insuranceFor(subtotal);
        BigDecimal orderTotal = subtotal.subtract(couponDiscount).add(shippingCost).add(insurance);
        payment.validateAvailability(orderTotal);
        PaymentCharge charge = payment.charge(new PaymentContext(orderTotal, installments, request.coupon() != null));

        return new OrderSummary(subtotal, couponDiscount, shippingCost, quote.days(), insurance,
                charge.finalTotal().subtract(orderTotal), charge.finalTotal(), installments,
                charge.installmentAmount(), tier.credit(subtotal), tier.gift(subtotal));
    }

    private void validateStructure(CheckoutRequest request) {
        if (request == null || request.items() == null || request.items().isEmpty()
                || request.customer() == null || isBlank(request.customer().cpf())
                || isBlank(request.customer().email())) {
            throw new ApiException(ErrorCode.INVALID_ORDER);
        }
        Set<String> seen = new HashSet<>();
        for (CheckoutRequest.Item item : request.items()) {
            if (item == null || isBlank(item.sku()) || item.quantity() == null || item.quantity() <= 0
                    || !seen.add(item.sku())) {
                throw new ApiException(ErrorCode.INVALID_ORDER);
            }
        }
    }

    private List<CartLine> loadLines(List<CheckoutRequest.Item> items) {
        Map<String, Product> found = products.findAllBySkus(items.stream().map(CheckoutRequest.Item::sku).toList())
                .stream().collect(Collectors.toMap(Product::sku, Function.identity()));
        if (items.stream().anyMatch(i -> !found.containsKey(i.sku()))) {
            throw new ApiException(ErrorCode.UNKNOWN_PRODUCT);
        }
        if (found.values().stream().anyMatch(Product::discontinued)) {
            throw new ApiException(ErrorCode.PRODUCT_DISCONTINUED);
        }
        if (items.stream().anyMatch(i -> found.get(i.sku()).stock() < i.quantity())) {
            throw new ApiException(ErrorCode.INSUFFICIENT_STOCK);
        }
        return items.stream().map(i -> new CartLine(found.get(i.sku()), i.quantity())).toList();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static <T> T find(Map<String, T> registry, String code, ErrorCode error) {
        T found = code == null ? null : registry.get(code);
        if (found == null) {
            throw new ApiException(error);
        }
        return found;
    }

    private static <T> Map<String, T> index(List<T> beans, Function<T, String> code) {
        Map<String, T> map = new LinkedHashMap<>();
        beans.forEach(b -> map.put(code.apply(b), b));
        return map;
    }
}
