package shop;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

record ShippingContext(String zipCode, BigDecimal weightKg, Region region) {
    long weightGrams() {
        return weightKg.multiply(BigDecimal.valueOf(1000)).setScale(0, java.math.RoundingMode.HALF_UP).longValueExact();
    }
}

record ShippingQuote(BigDecimal cost, int days) {}

/** Uma forma de entrega: cada nova opção é uma nova implementação registrada em {@link ShippingMethods}. */
interface ShippingMethod {
    String code();

    /** Verdadeiro se a opção tem frete (a retirada na loja não tem). */
    boolean chargesShipping();

    /** Verdadeiro se o frete pode ser isentado por vantagem do clube (falso para serviço terceirizado). */
    boolean waivableByClub();

    /** @throws ApiException SHIPPING_METHOD_UNAVAILABLE (422) ou CARRIER_UNAVAILABLE (503) */
    ShippingQuote quote(ShippingContext ctx);
}

record CarrierQuote(BigDecimal price, int etaHours) {}

class CarrierDownException extends RuntimeException {
    CarrierDownException(String message, Throwable cause) {
        super(message, cause);
    }
}

/** Transportadora externa. Optional vazio = CEP não atendido. */
interface CarrierGateway {
    Optional<CarrierQuote> quote(String zipCode, long weightGrams, String service) throws CarrierDownException;
}

class CarrierShippingMethod implements ShippingMethod {
    private final String code;
    private final String service;
    private final CarrierGateway gateway;

    CarrierShippingMethod(String code, String service, CarrierGateway gateway) {
        this.code = code;
        this.service = service;
        this.gateway = gateway;
    }

    public String code() { return code; }

    public boolean chargesShipping() { return true; }

    public boolean waivableByClub() { return true; }

    public ShippingQuote quote(ShippingContext ctx) {
        Optional<CarrierQuote> quote;
        try {
            quote = gateway.quote(ctx.zipCode(), ctx.weightGrams(), service);
        } catch (CarrierDownException e) {
            throw new ApiException("CARRIER_UNAVAILABLE", 503);
        }
        CarrierQuote q = quote.orElseThrow(() -> ApiException.unprocessable("SHIPPING_METHOD_UNAVAILABLE"));
        int days = (q.etaHours() + 23) / 24 + ctx.region().carrierExtraDays();
        return new ShippingQuote(Money.round(q.price()), days);
    }
}

class StorePickupShippingMethod implements ShippingMethod {
    public String code() { return "STORE_PICKUP"; }

    public boolean chargesShipping() { return false; }

    public boolean waivableByClub() { return true; }

    public ShippingQuote quote(ShippingContext ctx) {
        return new ShippingQuote(Money.ZERO, 1);
    }
}

class CourierShippingMethod implements ShippingMethod {
    private static final BigDecimal MAX_WEIGHT_KG = new BigDecimal("5");

    public String code() { return "COURIER"; }

    public boolean chargesShipping() { return true; }

    public boolean waivableByClub() { return false; }

    public ShippingQuote quote(ShippingContext ctx) {
        if (ctx.weightKg().compareTo(MAX_WEIGHT_KG) > 0) {
            throw ApiException.unprocessable("SHIPPING_METHOD_UNAVAILABLE");
        }
        return new ShippingQuote(new BigDecimal("18.00"), 0);
    }
}

final class ShippingMethods {
    private final Map<String, ShippingMethod> byCode = new LinkedHashMap<>();

    ShippingMethods(ShippingMethod... methods) {
        for (ShippingMethod m : methods) byCode.put(m.code(), m);
    }

    static ShippingMethods standard(CarrierGateway carrier) {
        return new ShippingMethods(
                new CarrierShippingMethod("ECONOMY", "STD", carrier),
                new CarrierShippingMethod("EXPRESS", "EXP", carrier),
                new StorePickupShippingMethod(),
                new CourierShippingMethod());
    }

    ShippingMethod get(String code) {
        return code == null ? null : byCode.get(code);
    }
}
