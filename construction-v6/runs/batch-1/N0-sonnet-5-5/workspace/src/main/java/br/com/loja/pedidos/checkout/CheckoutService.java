package br.com.loja.pedidos.checkout;

import br.com.loja.pedidos.catalog.Product;
import br.com.loja.pedidos.catalog.ProductRepository;
import br.com.loja.pedidos.club.ClubTier;
import br.com.loja.pedidos.common.ApiException;
import br.com.loja.pedidos.common.Money;
import br.com.loja.pedidos.coupon.Coupon;
import br.com.loja.pedidos.coupon.CouponContext;
import br.com.loja.pedidos.payment.PaymentCharge;
import br.com.loja.pedidos.payment.PaymentMethod;
import br.com.loja.pedidos.shipping.ShippingOption;
import br.com.loja.pedidos.shipping.ShippingQuote;
import br.com.loja.pedidos.shipping.ShippingRequest;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/** Computes the purchase summary. Used both for the preview and for registering an order. */
@Service
public class CheckoutService {

    private static final Pattern ZIP_CODE = Pattern.compile("[0-9]{8}");
    private static final BigDecimal COUPON_CAP = new BigDecimal("0.30");

    private final ProductRepository products;
    private final Map<String, ShippingOption> shippingOptions = new HashMap<>();
    private final Map<String, Coupon> coupons = new HashMap<>();
    private final Map<String, PaymentMethod> paymentMethods = new HashMap<>();

    public CheckoutService(ProductRepository products, List<ShippingOption> shippingOptions, List<Coupon> coupons,
                           List<PaymentMethod> paymentMethods) {
        this.products = products;
        shippingOptions.forEach(o -> this.shippingOptions.put(o.code(), o));
        coupons.forEach(c -> this.coupons.put(c.code(), c));
        paymentMethods.forEach(p -> this.paymentMethods.put(p.code(), p));
    }

    public OrderSummary summarize(CheckoutRequest request) {
        List<PricedItem> items = priceItems(request);

        ClubTier tier = ClubTier.parse(request.clubTier()).orElseThrow(() -> invalid("INVALID_CLUB_TIER"));
        Region region = Region.parse(request.region()).orElseThrow(() -> invalid("INVALID_REGION"));
        if (request.zipCode() == null || !ZIP_CODE.matcher(request.zipCode()).matches()) {
            throw invalid("INVALID_ZIP_CODE");
        }
        ShippingOption shipping = shippingOptions.get(request.shippingMethod());
        if (shipping == null) {
            throw invalid("INVALID_SHIPPING_METHOD");
        }

        BigDecimal subtotal = Money.round(items.stream().map(PricedItem::total).reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal weight = items.stream().map(i -> i.weightKg().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ShippingQuote quote = shipping.quote(new ShippingRequest(request.zipCode(), weight, region));
        BigDecimal shippingCost = tier.freeShipping() && shipping.clubFreeShippingApplies() ? Money.ZERO : quote.cost();

        BigDecimal couponDiscount = Money.ZERO;
        if (request.couponInformed()) {
            Coupon coupon = coupons.get(request.coupon());
            if (coupon == null) {
                throw invalid("INVALID_COUPON");
            }
            CouponContext context = new CouponContext(request.cpf(), items, subtotal, request.shippingMethod(),
                    shippingCost);
            if (!coupon.applicable(context)) {
                throw invalid("COUPON_NOT_APPLICABLE");
            }
            couponDiscount = Money.round(coupon.discount(context).min(Money.percent(subtotal, COUPON_CAP)));
        }

        BigDecimal insurance = region.insurance(subtotal);
        BigDecimal orderTotal = subtotal.subtract(couponDiscount).add(shippingCost).add(insurance);

        PaymentMethod payment = paymentMethods.get(request.paymentMethod());
        if (payment == null) {
            throw invalid("INVALID_PAYMENT_METHOD");
        }
        if (!payment.allowsInstallments(request.installments())) {
            throw invalid("INVALID_INSTALLMENTS");
        }
        if (!payment.availableFor(orderTotal)) {
            throw invalid("PAYMENT_METHOD_UNAVAILABLE");
        }
        PaymentCharge charge = payment.charge(orderTotal, request.installments(), request.couponInformed());

        return new OrderSummary(subtotal, couponDiscount, shippingCost, quote.days(), insurance,
                charge.finalTotal().subtract(orderTotal), charge.finalTotal(), request.installments(),
                charge.installmentAmount(), tier.nextPurchaseCredit(subtotal), tier.gift(subtotal));
    }

    /** Validates the cart (errors 1 to 4) and prices it with the current catalog. */
    private List<PricedItem> priceItems(CheckoutRequest request) {
        List<CheckoutRequest.Item> cart = request.items();
        if (isBlank(request.cpf()) || isBlank(request.email()) || cart == null || cart.isEmpty()) {
            throw invalid("INVALID_ORDER");
        }
        Set<String> skus = new HashSet<>();
        for (CheckoutRequest.Item item : cart) {
            if (isBlank(item.sku()) || item.quantity() == null || item.quantity() <= 0 || !skus.add(item.sku())) {
                throw invalid("INVALID_ORDER");
            }
        }
        Map<String, Product> catalog = products.findAll(skus).stream()
                .collect(java.util.stream.Collectors.toMap(Product::sku, Function.identity()));
        if (cart.stream().anyMatch(i -> !catalog.containsKey(i.sku()))) {
            throw invalid("UNKNOWN_PRODUCT");
        }
        if (cart.stream().anyMatch(i -> catalog.get(i.sku()).discontinued())) {
            throw invalid("PRODUCT_DISCONTINUED");
        }
        if (cart.stream().anyMatch(i -> catalog.get(i.sku()).stock() < i.quantity())) {
            throw invalid("INSUFFICIENT_STOCK");
        }
        return cart.stream().map(i -> {
            Product p = catalog.get(i.sku());
            return new PricedItem(p.sku(), p.price(), p.weightKg(), i.quantity());
        }).toList();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static ApiException invalid(String code) {
        return ApiException.unprocessable(code);
    }
}
