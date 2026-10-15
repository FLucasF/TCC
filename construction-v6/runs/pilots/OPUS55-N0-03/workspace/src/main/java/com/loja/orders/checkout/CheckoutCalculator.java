package com.loja.orders.checkout;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.loja.orders.catalog.Product;
import com.loja.orders.catalog.ProductRepository;
import com.loja.orders.common.ApiException;
import com.loja.orders.common.ErrorCode;
import com.loja.orders.common.Money;
import com.loja.orders.coupon.Coupon;
import com.loja.orders.coupon.CouponContext;
import com.loja.orders.coupon.Coupons;
import com.loja.orders.order.OrderItem;
import com.loja.orders.payment.PaymentContext;
import com.loja.orders.payment.PaymentMethod;
import com.loja.orders.payment.PaymentMethods;
import com.loja.orders.payment.PaymentTerms;
import com.loja.orders.shipping.ShippingOption;
import com.loja.orders.shipping.ShippingOptions;
import com.loja.orders.shipping.ShippingQuote;
import com.loja.orders.shipping.ShippingRequest;

/**
 * Calcula o resumo da compra, conferindo os dados na ordem combinada e recusando no primeiro
 * problema. O resumo mostrado antes de confirmar e o do pedido registrado saem daqui.
 */
@Service
public class CheckoutCalculator {

    private static final Pattern ZIP_CODE = Pattern.compile("\\d{8}");

    private final ProductRepository products;
    private final ShippingOptions shippingOptions;
    private final Coupons coupons;
    private final PaymentMethods paymentMethods;

    public CheckoutCalculator(ProductRepository products, ShippingOptions shippingOptions, Coupons coupons,
            PaymentMethods paymentMethods) {
        this.products = products;
        this.shippingOptions = shippingOptions;
        this.coupons = coupons;
        this.paymentMethods = paymentMethods;
    }

    public CheckoutSummary calculate(CheckoutRequest request) {
        List<CartItem> cart = cart(request.items());
        ClubTier tier = ClubTier.fromCode(request.clubTier()).orElseThrow(() -> invalid(ErrorCode.INVALID_CLUB_TIER));
        Region region = Region.fromCode(request.region()).orElseThrow(() -> invalid(ErrorCode.INVALID_REGION));
        if (request.zipCode() == null || !ZIP_CODE.matcher(request.zipCode()).matches()) {
            throw invalid(ErrorCode.INVALID_ZIP_CODE);
        }
        ShippingOption shippingOption = shippingOptions.find(request.shippingMethod())
                .orElseThrow(() -> invalid(ErrorCode.INVALID_SHIPPING_METHOD));

        // 1. Soma dos produtos
        BigDecimal subtotal = cart.stream().map(CartItem::total).reduce(Money.ZERO, BigDecimal::add);
        BigDecimal weightKg = cart.stream().map(CartItem::weightKg).reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Frete (calculado antes do cupom porque o cupom de frete depende dele)
        ShippingQuote quote = shippingOption.quote(new ShippingRequest(request.zipCode(), region, weightKg, subtotal));
        BigDecimal shippingCost = tier.freeShipping() && shippingOption.coveredByClubFreeShipping()
                ? Money.ZERO
                : Money.round(quote.cost());

        // 2. Desconto do cupom
        BigDecimal couponDiscount = Money.ZERO;
        if (request.couponInformed()) {
            Coupon coupon = coupons.find(request.coupon()).orElseThrow(() -> invalid(ErrorCode.INVALID_COUPON));
            couponDiscount = coupons.discount(coupon,
                    new CouponContext(request.cpf(), cart, subtotal, shippingOption, shippingCost));
        }

        // 4. Seguro e 5. total do pedido
        BigDecimal insurance = region.insurance(subtotal);
        BigDecimal orderTotal = subtotal.subtract(couponDiscount).add(shippingCost).add(insurance);

        // 6. Ajuste da forma de pagamento
        PaymentMethod paymentMethod = paymentMethods.find(request.paymentMethod())
                .orElseThrow(() -> invalid(ErrorCode.INVALID_PAYMENT_METHOD));
        Integer installments = request.installments();
        if (installments == null || !paymentMethod.acceptsInstallments(installments)) {
            throw invalid(ErrorCode.INVALID_INSTALLMENTS);
        }
        PaymentTerms terms = paymentMethod.terms(
                new PaymentContext(orderTotal, installments, request.couponInformed()));
        BigDecimal finalTotal = Money.round(terms.finalTotal());

        return new CheckoutSummary(
                subtotal,
                couponDiscount,
                shippingCost,
                quote.deliveryDays(),
                insurance,
                finalTotal.subtract(orderTotal),
                finalTotal,
                installments,
                Money.round(terms.installmentAmount()),
                tier.nextPurchaseCredit(subtotal),
                tier.gift(subtotal));
    }

    private List<CartItem> cart(List<OrderItem> items) {
        Map<String, Product> catalog = products.findBySkus(items.stream().map(OrderItem::sku).toList());
        if (items.stream().anyMatch(item -> !catalog.containsKey(item.sku()))) {
            throw invalid(ErrorCode.UNKNOWN_PRODUCT);
        }
        if (items.stream().anyMatch(item -> catalog.get(item.sku()).discontinued())) {
            throw invalid(ErrorCode.PRODUCT_DISCONTINUED);
        }
        if (items.stream().anyMatch(item -> catalog.get(item.sku()).stock() < item.quantity())) {
            throw invalid(ErrorCode.INSUFFICIENT_STOCK);
        }
        return items.stream().map(item -> new CartItem(catalog.get(item.sku()), item.quantity())).toList();
    }

    private static ApiException invalid(ErrorCode code) {
        return ApiException.unprocessable(code);
    }
}
