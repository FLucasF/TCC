package br.com.loja.shipping;

import java.math.RoundingMode;

import br.com.loja.common.ApiException;
import br.com.loja.common.ErrorCode;

/** Entrega cotada numa transportadora externa; o prazo ganha os dias extras de despacho da região. */
class CarrierShipping implements ShippingOption {

    private final String code;
    private final String carrierService;
    private final CarrierGateway gateway;

    CarrierShipping(String code, String carrierService, CarrierGateway gateway) {
        this.code = code;
        this.carrierService = carrierService;
        this.gateway = gateway;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public ShippingQuote quote(ShippingRequest request) {
        long grams = request.weightKg().movePointRight(3).setScale(0, RoundingMode.HALF_UP).longValueExact();
        try {
            CarrierGateway.CarrierQuote quote = gateway.quote(request.zipCode(), grams, carrierService);
            return new ShippingQuote(quote.price(), quote.days() + request.region().carrierExtraDays());
        } catch (CarrierGateway.ZipNotServedException e) {
            throw new ApiException(ErrorCode.SHIPPING_METHOD_UNAVAILABLE);
        } catch (CarrierGateway.CarrierDownException e) {
            throw new ApiException(ErrorCode.CARRIER_UNAVAILABLE);
        }
    }
}
