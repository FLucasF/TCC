package shop.domain.shipping;

import java.math.BigDecimal;
import shop.domain.DomainException;
import shop.domain.ErrorCode;

/** Entrega cotada na transportadora (econômica e expressa). */
public class CarrierShipping implements ShippingOption {
    private final String code;
    private final String carrierService;
    private final CarrierClient client;

    public CarrierShipping(String code, String carrierService, CarrierClient client) {
        this.code = code;
        this.carrierService = carrierService;
        this.client = client;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        long grams = request.weightKg().movePointRight(3).longValue();
        try {
            CarrierClient.CarrierQuote quote = client.quote(request.zipCode(), grams, carrierService);
            int days = (quote.etaHours() + 23) / 24 + request.region().extraCarrierDays();
            return new ShippingQuote(new BigDecimal(quote.priceCents()).movePointLeft(2), days);
        } catch (CarrierClient.ZipNotServedException e) {
            throw new DomainException(ErrorCode.SHIPPING_METHOD_UNAVAILABLE);
        } catch (CarrierClient.CarrierDownException e) {
            throw new DomainException(ErrorCode.CARRIER_UNAVAILABLE);
        }
    }

    @Override
    public boolean chargesFreight() {
        return true;
    }

    @Override
    public boolean clubWaivable() {
        return true;
    }
}
