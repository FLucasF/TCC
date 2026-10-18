package br.com.loja.pedidos.shipping;

import br.com.loja.pedidos.common.ApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Delivery priced and timed by an external carrier; the dispatch delay of the region is added on top. */
public class CarrierShippingOption implements ShippingOption {

    private final String code;
    private final ServiceLevel level;
    private final CarrierGateway carrier;

    public CarrierShippingOption(String code, ServiceLevel level, CarrierGateway carrier) {
        this.code = code;
        this.level = level;
        this.carrier = carrier;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        long grams = request.weightKg().multiply(BigDecimal.valueOf(1000)).setScale(0, RoundingMode.CEILING)
                .longValueExact();
        return switch (carrier.quote(request.zipCode(), grams, level)) {
            case CarrierGateway.Quoted q -> new ShippingQuote(q.price(),
                    (int) ((q.etaHours() + 23) / 24) + request.region().dispatchDelayDays());
            case CarrierGateway.NotServed n -> throw ApiException.unprocessable("SHIPPING_METHOD_UNAVAILABLE");
            case CarrierGateway.Unavailable u -> throw new ApiException("CARRIER_UNAVAILABLE", 503);
        };
    }
}
