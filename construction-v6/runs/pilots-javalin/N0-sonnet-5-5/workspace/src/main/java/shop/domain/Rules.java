package shop.domain;

import java.math.BigDecimal;
import java.util.List;

/** The business rules currently in force, keyed by the codes the API uses. */
public record Rules(Registry<ClubTier> tiers, Registry<ShippingMethod> shipping, Registry<Coupon> coupons,
                    Registry<PaymentMethod> payments, BigDecimal couponDiscountCap) {

    public static Rules standard(CarrierGateway carrier) {
        List<ClubTier> tiers = List.of(
                new ClubTier("BRONZE", new BigDecimal("0"), false, null),
                new ClubTier("SILVER", new BigDecimal("0.02"), false, null),
                new ClubTier("GOLD", new BigDecimal("0.05"), true, Money.of("500.00")));
        List<ShippingMethod> shipping = List.of(
                new ShippingMethod.Carrier("ECONOMY", carrier, "STD"),
                new ShippingMethod.Carrier("EXPRESS", carrier, "EXP"),
                new ShippingMethod.Fixed("STORE_PICKUP", Money.ZERO, 1, false, true, null),
                new ShippingMethod.Fixed("COURIER", Money.of("18.00"), 0, true, false, new BigDecimal("5")));
        List<Coupon> coupons = List.of(
                new Coupon.PercentOfProducts("WELCOME10", new BigDecimal("0.10"), true),
                new Coupon.FixedOffProducts("MINUS50", Money.of("50.00"), Money.of("300.00")),
                new Coupon.FreeShipping("FREESHIPPING"),
                new Coupon.EveryNthFree("BUY3PAY2", 3));
        List<PaymentMethod> payments = List.of(
                new PaymentMethod.Pix("PIX", new BigDecimal("0.05")),
                new PaymentMethod.CreditCard("CREDIT_CARD", 12, 3, new BigDecimal("0.0199")),
                new PaymentMethod.BankSlip("BANK_SLIP", Money.of("3.49"), Money.of("1000.00")));
        return new Rules(
                new Registry<>(ClubTier::code, tiers),
                new Registry<>(ShippingMethod::code, shipping),
                new Registry<>(Coupon::code, coupons),
                new Registry<>(PaymentMethod::code, payments),
                new BigDecimal("0.30"));
    }
}
